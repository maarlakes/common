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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.*;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public class JdkHttpClient implements HttpClient {

    private final Executor executor;
    private final boolean ownsExecutor;

    public JdkHttpClient() {
        this(new ForkJoinPool(), true);
    }

    public JdkHttpClient(@Nonnull Executor executor) {
        this(executor, false);
    }

    private JdkHttpClient(@Nonnull Executor executor, boolean ownsExecutor) {
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
    }

    @Nonnull
    @Override
    public CompletionStage<? extends Response> execute(@Nonnull Request request, RequestConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                final URL url = toUrl(request);
                HttpURLConnection connection = createConnection(url, request, config);
                Response response = doExecute(connection, request, url);
                if (response.getStatusCode() == 407 && config != null && config.getProxy() != null && config.getProxyAuthentication() != null) {
                    connection = createConnection(url, request, config);
                    boolean isProxy = false;
                    for (ProxyAuthenticator authenticator : SpiServiceLoader.loadShared(ProxyAuthenticator.class, this.getClass().getClassLoader())) {
                        if (authenticator.supported(config.getProxy(), config.getProxyAuthentication())) {
                            isProxy = authenticator.authenticate(connection, response, config.getProxy(), config.getProxyAuthentication());
                            break;
                        }
                    }
                    if (isProxy) {
                        response = doExecute(connection, request, url);
                    }
                }

                return response;
            } catch (Exception e) {
                throw new HttpClientException(e.getMessage(), e);
            }
        }, this.executor);
    }

    @Override
    public void close() throws IOException {
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
            RequestBody<?> body = request.getBody();
            if (body == null && CollectionUtils.isNotEmpty(request.getFormParams())) {
                body = new UrlEncodedFormEntityBody(request.getFormParams(), Optional.ofNullable(request.getCharset()).map(Charset::name).orElse("utf-8"));
            }
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty(HttpHeaderNames.CONTENT_TYPE, body.getContentTypeHeader().get());
                try (OutputStream out = connection.getOutputStream()) {
                    body.writeTo(out);
                }
            }
            connection.connect();
            if (connection.getResponseCode() == 200) {
                try (InputStream in = connection.getInputStream()) {
                    return new DefaultResponse(url, connection.getResponseCode(), connection.getResponseMessage(), StreamUtils.readAllBytes(in), request.getUri(), toHeaders(connection.getHeaderFields()));
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
        connection.setInstanceFollowRedirects(config == null || config.isRedirectsEnabled());
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
                final int port = url.getPort();
                return new InetSocketAddress(url.getHost(), port == -1 ? url.getDefaultPort() : port);
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
            Header header = this.getHeaders().getHeader("Set-Cookie");
            final List<Cookie> cookies = new ArrayList<>();
            if (header != null && CollectionUtils.isNotEmpty(header.getValues())) {
                for (String value : header.getValues()) {
                    final Cookie cookie = Cookies.parse(value);
                    if (cookie != null) {
                        cookies.add(cookie);
                    }
                }
            }
            header = this.getHeaders().getHeader("set-cookie2");
            if (header != null && CollectionUtils.isNotEmpty(header.getValues())) {
                for (String value : header.getValues()) {
                    final Cookie cookie = Cookies.parse(value);
                    if (cookie != null) {
                        cookies.add(cookie);
                    }
                }
            }

            return cookies;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return this.socketAddress.get();
        }
    }
}
