package cn.maarlakes.common.http.jdk;

import cn.maarlakes.common.http.*;
import cn.maarlakes.common.http.body.BodyUtils;
import cn.maarlakes.common.http.body.UrlEncodedFormEntityBody;
import cn.maarlakes.common.http.proxy.ProxyAuthentication;
import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.CollectionUtils;
import cn.maarlakes.common.utils.Lazy;
import cn.maarlakes.common.utils.StreamUtils;
import jakarta.annotation.Nonnull;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public class JdkHttpClient implements HttpClient {

    private final Executor executor;
    private final boolean ownsExecutor;
    private final SSLContext sslContext;
    private final RequestConfig defaultConfig;

    public JdkHttpClient() {
        this(new ForkJoinPool(), true, null, null);
    }

    public JdkHttpClient(RequestConfig defaultConfig) {
        this(new ForkJoinPool(), true, null, defaultConfig);
    }

    public JdkHttpClient(@Nonnull Executor executor) {
        this(executor, false, null, null);
    }

    public JdkHttpClient(@Nonnull Executor executor, RequestConfig defaultConfig) {
        this(executor, false, null, defaultConfig);
    }

    public JdkHttpClient(@Nonnull Executor executor, SSLContext sslContext) {
        this(executor, false, sslContext, null);
    }

    public JdkHttpClient(@Nonnull Executor executor, SSLContext sslContext, RequestConfig defaultConfig) {
        this(executor, false, sslContext, defaultConfig);
    }

    JdkHttpClient(@Nonnull Executor executor, boolean ownsExecutor, SSLContext sslContext, RequestConfig defaultConfig) {
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
        this.sslContext = sslContext;
        this.defaultConfig = defaultConfig;
    }

    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        final ResponseFuture future = new ResponseFuture();
        this.executor.execute(() -> {
            try {
                if (future.isCancelled()) {
                    return;
                }
                final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
                final URL url = toUrl(request);
                HttpURLConnection connection = createConnection(url, request, effectiveConfig);
                future.setConnection(connection);
                Response response = doExecute(connection, request, url);
                if (!future.isCancelled() && response.getStatusCode() == 407 && config != null && config.getProxy() != null && config.getProxyAuthentication() != null) {
                    connection = createConnection(url, request, config);
                    future.setConnection(connection);
                    if (future.isCancelled()) {
                        return;
                    }
                    boolean isProxy = false;
                    for (ProxyAuthenticator authenticator : SpiServiceLoader.loadShared(ProxyAuthenticator.class, this.getClass().getClassLoader())) {
                        if (authenticator.supported(config.getProxy(), config.getProxyAuthentication())) {
                            isProxy = authenticator.authenticate(connection, response, config.getProxy(), config.getProxyAuthentication());
                            break;
                        }
                    }
                    if (!isProxy) {
                        return;
                    }
                    response = doExecute(connection, request, url);
                }

                if (!future.isCancelled()) {
                    future.complete(response);
                }
            } catch (Exception e) {
                if (!future.isCancelled()) {
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }
            }
        });
        return future;
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        final HandlerFuture<T> future = new HandlerFuture<>();
        this.executor.execute(() -> {
            try {
                if (future.isCancelled()) {
                    return;
                }
                final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
                final URL url = toUrl(request);
                HttpURLConnection connection = createConnection(url, request, effectiveConfig);
                future.setConnection(connection);
                HttpResponse httpResponse = doExecuteHandler(connection, request, url);
                if (!future.isCancelled() && httpResponse.getStatusCode() == 407 && config != null && config.getProxy() != null && config.getProxyAuthentication() != null) {
                    connection = createConnection(url, request, config);
                    future.setConnection(connection);
                    if (future.isCancelled()) {
                        return;
                    }
                    boolean isProxy = false;
                    Response tempResponse = new TempResponse(httpResponse);
                    for (ProxyAuthenticator authenticator : SpiServiceLoader.loadShared(ProxyAuthenticator.class, this.getClass().getClassLoader())) {
                        if (authenticator.supported(config.getProxy(), config.getProxyAuthentication())) {
                            isProxy = authenticator.authenticate(connection, tempResponse, config.getProxy(), config.getProxyAuthentication());
                            break;
                        }
                    }
                    if (!isProxy) {
                        return;
                    }
                    httpResponse = doExecuteHandler(connection, request, url);
                }

                if (!future.isCancelled()) {
                    final BodySink body = new JdkBodySink(connection);
                    final CompletableFuture<T> result = handler.handle(httpResponse, body);
                    result.whenComplete((val, err) -> {
                        if (err != null) {
                            future.completeExceptionally(err);
                        } else {
                            future.complete(val);
                        }
                    });
                }
            } catch (Exception e) {
                if (!future.isCancelled()) {
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }
            }
        });
        return future;
    }

    @Override
    public void close() {
        if (this.ownsExecutor && this.executor instanceof ExecutorService) {
            ((ExecutorService) executor).shutdown();
        }
    }

    private static URL toUrl(@Nonnull Request request) throws Exception {
        if (CollectionUtils.isEmpty(request.getQueryParams())) {
            return request.getUri().toURL();
        }
        final String url = request.getUri().toString();
        if (url.contains("?")) {
            if (url.endsWith("&")) {
                return new URL(url + BodyUtils.formatParamsEncode(request.getQueryParams()));
            }
            return new URL(url + "&" + BodyUtils.formatParamsEncode(request.getQueryParams()));
        }
        return new URL(url + "?" + BodyUtils.formatParamsEncode(request.getQueryParams()));
    }

    private HttpHeaders toHeaders(final Map<String, List<String>> map) {
        return new DefaultHttpHeaders(
                map.entrySet().stream().filter(item -> item.getKey() != null).map(entry -> new DefaultHeader(entry.getKey(), entry.getValue())).collect(Collectors.toList())
                        .stream().collect(Collectors.toMap(DefaultHeader::getName, Function.identity()))
        );
    }

    private Response doExecute(HttpURLConnection connection, Request request, URL url) throws Exception {
        try {
            sendRequestBody(connection, request);
            connection.connect();
            final int responseCode = connection.getResponseCode();

            if (responseCode >= 200 && responseCode < 300) {
                try (InputStream in = connection.getInputStream()) {
                    return new DefaultResponse(url, responseCode, connection.getResponseMessage(), StreamUtils.readAllBytes(in), request.getUri(), toHeaders(connection.getHeaderFields()));
                }
            }
            try (InputStream in = connection.getErrorStream()) {
                return new DefaultResponse(url, connection.getResponseCode(), connection.getResponseMessage(), in == null ? null : StreamUtils.readAllBytes(in), request.getUri(), toHeaders(connection.getHeaderFields()));
            }
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }

    private HttpResponse doExecuteHandler(HttpURLConnection connection, Request request, URL url) throws Exception {
        sendRequestBody(connection, request);
        connection.connect();
        final int responseCode = connection.getResponseCode();
        final HttpHeaders headers = toHeaders(connection.getHeaderFields());
        final int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
        final SocketAddress remoteAddress = new InetSocketAddress(InetAddress.getByName(url.getHost()), port);
        return new DefaultHttpResponse(
                responseCode,
                connection.getResponseMessage(),
                headers,
                request.getUri(),
                Cookies.parseFromHeaders(headers),
                remoteAddress
        );
    }

    private void sendRequestBody(HttpURLConnection connection, Request request) throws Exception {
        RequestBody<?> body = request.getBody();
        if (body == null && CollectionUtils.isNotEmpty(request.getFormParams())) {
            body = new UrlEncodedFormEntityBody(request.getFormParams(), Optional.ofNullable(request.getCharset()).map(Charset::name).orElse("utf-8"));
        }
        if (body != null) {
            connection.setDoOutput(true);
            final Header contentTypeHeader = body.getContentTypeHeader();
            if (contentTypeHeader != null) {
                connection.setRequestProperty(HttpHeaderNames.CONTENT_TYPE, contentTypeHeader.get());
            }
            try (OutputStream out = connection.getOutputStream()) {
                body.writeTo(out);
            }
        }
    }

    private HttpURLConnection createConnection(@Nonnull URL url, @Nonnull Request request, RequestConfig config) throws Exception {
        HttpURLConnection connection;
        if (config != null && config.getProxy() != null) {
            connection = (HttpURLConnection) url.openConnection(config.getProxy());
            if (config.getProxyAuthentication() != null) {
                this.setProxyAuthorization(connection, config.getProxy(), config.getProxyAuthentication());
            }

        } else {
            connection = (HttpURLConnection) url.openConnection();
        }
        connection.setDoInput(true);
        if (config != null && config.isRedirectsEnabled() != null) {
            connection.setInstanceFollowRedirects(config.isRedirectsEnabled());
        }
        connection.setRequestMethod(request.getMethod().name());
        if (config != null) {
            if (config.getConnectTimeout() != null) {
                connection.setConnectTimeout((int) config.getConnectTimeout().toMillis());
            } else if (config.getRequestTimeout() != null) {
                connection.setConnectTimeout((int) config.getRequestTimeout().toMillis());
            }
            if (config.getResponseTimeout() != null) {
                connection.setReadTimeout((int) config.getResponseTimeout().toMillis());
            }
        }
        if (!request.getHeaders().isEmpty()) {
            for (Header header : request.getHeaders()) {
                connection.setRequestProperty(header.getName(), header.get());
            }
        }
        if (CollectionUtils.isNotEmpty(request.getCookies())) {
            connection.setRequestProperty(HttpHeaderNames.COOKIE, request.getCookies().stream().map(item -> item.name() + "=" + item.value()).collect(Collectors.joining(";")));
        }
        if (connection instanceof HttpsURLConnection) {
            if (this.sslContext != null) {
                ((HttpsURLConnection) connection).setSSLSocketFactory(this.sslContext.getSocketFactory());
            }
        }
        return connection;
    }

    private void setProxyAuthorization(@Nonnull HttpURLConnection connection, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        for (ProxyAuthenticator authenticator : SpiServiceLoader.loadShared(ProxyAuthenticator.class, this.getClass().getClassLoader())) {
            if (authenticator.supported(proxy, authentication)) {
                authenticator.authenticate(connection, proxy, authentication);
                break;
            }
        }
    }

    private static class ResponseFuture extends CompletableFuture<Response> {
        private volatile HttpURLConnection connection;

        void setConnection(@Nonnull HttpURLConnection connection) {
            if (this.isCancelled()) {
                connection.disconnect();
            }
            this.connection = connection;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            final HttpURLConnection connection = this.connection;
            if (connection != null) {
                connection.disconnect();
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static class HandlerFuture<T> extends CompletableFuture<T> {
        private volatile HttpURLConnection connection;

        void setConnection(@Nonnull HttpURLConnection connection) {
            if (this.isCancelled()) {
                connection.disconnect();
            }
            this.connection = connection;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            final HttpURLConnection connection = this.connection;
            if (connection != null) {
                connection.disconnect();
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static class JdkBodySink implements BodySink {
        private final HttpURLConnection connection;

        private JdkBodySink(HttpURLConnection connection) {
            this.connection = connection;
        }

        @Override
        public <T> CompletableFuture<T> consume(@Nonnull BodyConsumer<T> consumer) {
            final CompletableFuture<T> future = new CompletableFuture<>();
            try {
                final int statusCode = this.connection.getResponseCode();
                final InputStream stream = (statusCode >= 200 && statusCode < 300)
                        ? this.connection.getInputStream()
                        : this.connection.getErrorStream();
                if (stream != null) {
                    try {
                        final byte[] buffer = new byte[8192];
                        int n;
                        while ((n = stream.read(buffer)) != -1) {
                            consumer.onChunk(buffer, 0, n);
                        }
                    } finally {
                        stream.close();
                    }
                }
                future.complete(consumer.onComplete());
            } catch (Exception e) {
                try {
                    consumer.onError(e);
                } catch (Exception onErrorEx) {
                    e.addSuppressed(onErrorEx);
                }
                future.completeExceptionally(e);
            } finally {
                this.connection.disconnect();
            }
            return future;
        }
    }

    private static class TempResponse implements Response {
        private final HttpResponse info;

        private TempResponse(HttpResponse response) {
            this.info = response;
        }

        @Override
        public int getStatusCode() {
            return this.info.getStatusCode();
        }

        @Override
        public String getStatusText() {
            return this.info.getStatusText();
        }

        @Nonnull
        @Override
        public ResponseBody getBody() {
            return new ByteArrayResponseBody(new byte[0], null, null);
        }

        @Override
        public URI getUri() {
            return this.info.getUri();
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            return this.info.getHeaders();
        }

        @Nonnull
        @Override
        public List<? extends Cookie> getCookies() {
            return this.info.getCookies();
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return this.info.getRemoteAddress();
        }
    }

    private static class DefaultResponse implements Response {
        private final Lazy<SocketAddress> socketAddress;
        private final int statusCode;
        private final String statusText;
        private final URI uri;
        private final HttpHeaders headers;
        private final ResponseBody body;

        private DefaultResponse(URL url, int statusCode, String statusText, byte[] body, URI uri, HttpHeaders headers) {
            this.statusCode = statusCode;
            this.statusText = statusText;
            this.uri = uri;
            this.headers = headers;

            this.body = new ByteArrayResponseBody(
                    body == null ? new byte[0] : body,
                    Optional.ofNullable(this.headers.getHeader(HttpHeaderNames.CONTENT_TYPE).get()).map(ContentType::parse).orElse(null),
                    headers.getHeader(HttpHeaderNames.CONTENT_ENCODING)
            );
            this.socketAddress = Lazy.of(() -> {
                final int port = url.getPort() == -1 ? url.getDefaultPort() : url.getPort();
                return new InetSocketAddress(InetAddress.getByName(url.getHost()), port);
            });
        }

        @Override
        public int getStatusCode() {
            return this.statusCode;
        }

        @Override
        public String getStatusText() {
            return this.statusText;
        }

        @Nonnull
        @Override
        public ResponseBody getBody() {
            return this.body;
        }

        @Override
        public URI getUri() {
            return this.uri;
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            return this.headers;
        }

        @Nonnull
        @Override
        public List<? extends Cookie> getCookies() {
            return Cookies.parseFromHeaders(this.headers);
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return this.socketAddress.get();
        }
    }
}
