package cn.maarlakes.common.http.ok;

import cn.maarlakes.common.http.*;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.http.ResponseBody;
import cn.maarlakes.common.http.body.multipart.MultipartPart;
import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.CollectionUtils;
import jakarta.annotation.Nonnull;
import okhttp3.*;
import okhttp3.Cookie;
import okhttp3.RequestBody;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
public class OkAsyncHttpClient implements HttpClient {

    private final OkHttpClient client;
    private final SSLContext sslContext;
    private final RequestConfig defaultConfig;

    public OkAsyncHttpClient() {
        this(new OkHttpClient.Builder().build(), null, null);
    }

    public OkAsyncHttpClient(RequestConfig defaultConfig) {
        this(new OkHttpClient.Builder().build(), null, defaultConfig);
    }

    public OkAsyncHttpClient(@Nonnull OkHttpClient client) {
        this(client, null, null);
    }

    public OkAsyncHttpClient(@Nonnull OkHttpClient client, RequestConfig defaultConfig) {
        this(client, null, defaultConfig);
    }

    public OkAsyncHttpClient(@Nonnull OkHttpClient client, SSLContext sslContext) {
        this(client, sslContext, null);
    }

    public OkAsyncHttpClient(@Nonnull OkHttpClient client, SSLContext sslContext, RequestConfig defaultConfig) {
        this.client = client;
        this.sslContext = sslContext;
        this.defaultConfig = defaultConfig;
    }

    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
        try {
            final HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.get(request.getUri())).newBuilder();
            if (CollectionUtils.isNotEmpty(request.getQueryParams())) {
                for (NameValuePair param : request.getQueryParams()) {
                    builder.addQueryParameter(param.getName(), param.getValue());
                }
            }
            final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder().method(request.getMethod().name(), toRequestBody(request)).url(builder.build());
            if (!request.getHeaders().isEmpty()) {
                for (Header header : request.getHeaders()) {
                    for (String value : header.getValues()) {
                        requestBuilder.addHeader(header.getName(), value);
                    }
                }
            }
            final List<cn.maarlakes.common.http.Cookie> responseCookies = new ArrayList<>();
            final OkHttpClient client = this.getClient(request, responseCookies, effectiveConfig);
            final Call call = client.newCall(requestBuilder.build());
            final ResponseFuture future = new ResponseFuture(call);

            call.enqueue(new Callback() {
                @Override
                public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }

                @Override
                public void onResponse(@Nonnull Call call, @Nonnull okhttp3.Response response) throws IOException {
                    future.complete(new DefaultResponse(response, responseCookies));
                }
            });
            return future;
        } catch (Exception e) {
            return CompletableFuture.supplyAsync(() -> {
                throw new HttpClientException(e.getMessage(), e);
            });
        }
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
        try {
            final HttpUrl.Builder builder = Objects.requireNonNull(HttpUrl.get(request.getUri())).newBuilder();
            if (CollectionUtils.isNotEmpty(request.getQueryParams())) {
                for (NameValuePair param : request.getQueryParams()) {
                    builder.addQueryParameter(param.getName(), param.getValue());
                }
            }
            final okhttp3.Request.Builder requestBuilder = new okhttp3.Request.Builder().method(request.getMethod().name(), toRequestBody(request)).url(builder.build());
            if (!request.getHeaders().isEmpty()) {
                for (Header header : request.getHeaders()) {
                    for (String value : header.getValues()) {
                        requestBuilder.addHeader(header.getName(), value);
                    }
                }
            }
            final List<cn.maarlakes.common.http.Cookie> responseCookies = new ArrayList<>();
            final OkHttpClient client = this.getClient(request, responseCookies, effectiveConfig);
            final Call call = client.newCall(requestBuilder.build());
            final HandlerFuture<T> future = new HandlerFuture<>(call);

            call.enqueue(new Callback() {
                @Override
                public void onFailure(@Nonnull Call call, @Nonnull IOException e) {
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }

                @Override
                public void onResponse(@Nonnull Call call, @Nonnull okhttp3.Response response) throws IOException {
                    try {
                        final HttpResponse info = createResponseInfo(response, responseCookies);
                        final BodySink body = new OkBodySink(response.body());
                        final CompletableFuture<T> result = handler.handle(info, body);
                        result.whenComplete((val, err) -> {
                            if (err != null) {
                                future.completeExceptionally(err);
                            } else {
                                future.complete(val);
                            }
                        });
                    } catch (Exception e) {
                        future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                    }
                }
            });
            return future;
        } catch (Exception e) {
            final CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(new HttpClientException(e.getMessage(), e));
            return future;
        }
    }

    private OkHttpClient getClient(@Nonnull Request request, final List<cn.maarlakes.common.http.Cookie> responseCookies, RequestConfig config) {
        final List<? extends cn.maarlakes.common.http.Cookie> cookies = request.getCookies();
        final OkHttpClient.Builder builder = client.newBuilder().cookieJar(new CookieJar() {
            @Override
            public void saveFromResponse(@Nonnull HttpUrl httpUrl, @Nonnull List<Cookie> list) {
                if (CollectionUtils.isNotEmpty(list)) {
                    for (Cookie cookie : list) {
                        final cn.maarlakes.common.http.Cookie.Builder builder = cn.maarlakes.common.http.Cookie.builder(cookie.name())
                                .value(cookie.value())
                                .domain(cookie.domain())
                                .path(cookie.path());
                        if (cookie.expiresAt() > 0) {
                            builder.maxAge((cookie.expiresAt() - System.currentTimeMillis()) / 1000L);
                        }
                        builder.isSecure(cookie.secure());
                        builder.isHttpOnly(cookie.hostOnly());
                        responseCookies.add(builder.build());
                    }
                }
            }

            @Nonnull
            @Override
            public List<Cookie> loadForRequest(@Nonnull HttpUrl httpUrl) {
                final List<Cookie> list = new ArrayList<>();
                for (cn.maarlakes.common.http.Cookie cookie : cookies) {
                    final Cookie.Builder cookieBuilder = new Cookie.Builder()
                            .name(cookie.name())
                            .value(cookie.value());
                    if (cookie.domain() != null) {
                        cookieBuilder.domain(cookie.domain());
                    } else {
                        cookieBuilder.domain(httpUrl.host());
                    }
                    if (cookie.path() != null) {
                        cookieBuilder.path(cookie.path());
                    }
                    if (cookie.maxAge() > 0L) {
                        cookieBuilder.expiresAt(System.currentTimeMillis() + cookie.maxAge() * 1000);
                    }
                    if (cookie.isSecure()) {
                        cookieBuilder.secure();
                    }
                    if (cookie.isHttpOnly()) {
                        cookieBuilder.httpOnly();
                    }
                    list.add(cookieBuilder.build());
                }
                return list;
            }
        });
        if (config != null) {
            if (config.getConnectTimeout() != null) {
                builder.connectTimeout(config.getConnectTimeout());
            }
            if (config.getRequestTimeout() != null) {
                builder.writeTimeout(config.getRequestTimeout());
            }
            if (config.getResponseTimeout() != null) {
                builder.readTimeout(config.getResponseTimeout());
            }
            if (config.getProxy() != null) {
                builder.proxy(config.getProxy());
                if (config.getProxyAuthentication() != null) {
                    final List<Authenticator> authenticators = new ArrayList<>();
                    for (ProxyAuthenticator authenticator : SpiServiceLoader.loadShared(ProxyAuthenticator.class, this.getClass().getClassLoader())) {
                        final Authenticator a = authenticator.authenticate(config.getProxy(), config.getProxyAuthentication());
                        if (a != null) {
                            authenticators.add(a);
                        }
                    }
                    if (!authenticators.isEmpty()) {
                        builder.proxyAuthenticator((route, response) -> {
                            for (Authenticator a : authenticators) {
                                final okhttp3.Request newRequest = a.authenticate(route, response);
                                if (newRequest != null) {
                                    return newRequest;
                                }
                            }
                            return null;
                        });
                    }
                }
            }
        }
        if (this.sslContext != null) {
            builder.sslSocketFactory(this.sslContext.getSocketFactory());
        }
        return builder.build();
    }

    private static RequestBody toRequestBody(@Nonnull Request request) throws Exception {
        final cn.maarlakes.common.http.RequestBody<?> body = request.getBody();
        if (body != null) {
            if (body instanceof cn.maarlakes.common.http.body.multipart.MultipartBody) {
                final cn.maarlakes.common.http.body.multipart.MultipartBody multipartBody = (cn.maarlakes.common.http.body.multipart.MultipartBody) request.getBody();
                if (CollectionUtils.isNotEmpty(multipartBody.getContent())) {
                    final MultipartBody.Builder builder = new MultipartBody.Builder();
                    if (multipartBody.getContentType() != null) {
                        builder.setType(Objects.requireNonNull(MediaType.parse(ContentTypes.toString(multipartBody.getContentType()))));
                    }
                    for (MultipartPart<?> part : multipartBody.getContent()) {
                        final Headers.Builder headers = new Headers.Builder();
                        if (!part.getHeaders().isEmpty()) {
                            for (Header header : part.getHeaders()) {
                                for (String value : header.getValues()) {
                                    headers.add(header.getName(), value);
                                }
                            }
                        }
                        builder.addFormDataPart(part.getName(), part.getFilename(), new ContentRequestBody(part));
                    }
                    return builder.build();
                }
            } else {
                return new ContentRequestBody(body);
            }
        }

        if (CollectionUtils.isNotEmpty(request.getFormParams())) {
            final FormBody.Builder builder = new FormBody.Builder();
            for (NameValuePair param : request.getFormParams()) {
                builder.add(param.getName(), param.getValue());
            }
            return builder.build();
        }
        return null;
    }

    @Override
    public void close() throws IOException {
    }

    private static class ResponseFuture extends CompletableFuture<Response> {
        private final Call call;

        private ResponseFuture(Call call) {
            this.call = call;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (mayInterruptIfRunning) {
                this.call.cancel();
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static class HandlerFuture<T> extends CompletableFuture<T> {
        private final Call call;

        private HandlerFuture(Call call) {
            this.call = call;
        }

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (mayInterruptIfRunning) {
                this.call.cancel();
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }

    private static HttpResponse createResponseInfo(@Nonnull okhttp3.Response response, List<cn.maarlakes.common.http.Cookie> cookies) throws Exception {
        final HttpUrl url = response.request().url();
        final int port = url.port();
        final SocketAddress remoteAddress = new InetSocketAddress(InetAddress.getByName(url.host()), port);
        final Set<String> names = response.headers().names();
        final HttpHeaders headers = new HttpHeaders() {
            @Override
            public boolean isEmpty() {
                return names.isEmpty();
            }

            @Override
            public Header getHeader(@Nonnull String name) {
                return new DefaultHeader(name, response.headers(name));
            }

            @Nonnull
            @Override
            public Iterator<Header> iterator() {
                final Iterator<String> iterator = names.iterator();
                return new Iterator<Header>() {
                    @Override
                    public boolean hasNext() {
                        return iterator.hasNext();
                    }

                    @Override
                    public Header next() {
                        return getHeader(iterator.next());
                    }
                };
            }

            @Override
            public String toString() {
                return response.headers().toString();
            }
        };
        return new DefaultHttpResponse(
                response.code(),
                response.message(),
                headers,
                url.uri(),
                new ArrayList<>(cookies),
                remoteAddress
        );
    }

    private static class OkBodySink implements BodySink {
        private final okhttp3.ResponseBody responseBody;

        private OkBodySink(okhttp3.ResponseBody responseBody) {
            this.responseBody = responseBody;
        }

        @Override
        public <T> CompletableFuture<T> consume(@Nonnull BodyConsumer<T> consumer) {
            final CompletableFuture<T> future = new CompletableFuture<>();
            if (this.responseBody == null) {
                try {
                    future.complete(consumer.onComplete());
                } catch (Exception e) {
                    try {
                        consumer.onError(e);
                    } catch (Exception onErrorEx) {
                        e.addSuppressed(onErrorEx);
                    }
                    future.completeExceptionally(e);
                }
                return future;
            }
            try (InputStream in = this.responseBody.byteStream()) {
                final byte[] buffer = new byte[8192];
                int n;
                while ((n = in.read(buffer)) != -1) {
                    consumer.onChunk(buffer, 0, n);
                }
                future.complete(consumer.onComplete());
            } catch (Exception e) {
                try {
                    consumer.onError(e);
                } catch (Exception onErrorEx) {
                    e.addSuppressed(onErrorEx);
                }
                future.completeExceptionally(e);
            }
            return future;
        }
    }

    private static class DefaultResponse implements Response {

        private final okhttp3.Response response;
        private final List<cn.maarlakes.common.http.Cookie> cookies;
        private final ResponseBody body;

        private DefaultResponse(@Nonnull okhttp3.Response response, List<cn.maarlakes.common.http.Cookie> cookies) {
            this.response = response;
            this.cookies = cookies;

            final okhttp3.ResponseBody responseBody = response.body();
            if (responseBody == null) {
                this.body = new ByteArrayResponseBody(new byte[0], null, null);
                response.close();
            } else {
                try {
                    this.body = new ByteArrayResponseBody(
                            responseBody.bytes(),
                            Optional.ofNullable(responseBody.contentType()).map(MediaType::toString).map(ContentType::parse).orElse(null),
                            this.getHeaders().getHeader(HttpHeaderNames.CONTENT_ENCODING)
                    );
                } catch (IOException e) {
                    throw new HttpClientException(e.getMessage(), e);
                } finally {
                    response.close();
                }
            }
        }

        @Override
        public int getStatusCode() {
            return this.response.code();
        }

        @Override
        public String getStatusText() {
            return this.response.message();
        }

        @Nonnull
        @Override
        public ResponseBody getBody() {
            return this.body;
        }

        @Override
        public URI getUri() {
            return this.response.request().url().uri();
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            final Set<String> names = this.response.headers().names();
            return new HttpHeaders() {
                @Override
                public boolean isEmpty() {
                    return names.isEmpty();
                }

                @Override
                public Header getHeader(@Nonnull String name) {
                    return new DefaultHeader(name, response.headers(name));
                }

                @Nonnull
                @Override
                public Iterator<Header> iterator() {
                    final Iterator<String> iterator = names.iterator();
                    return new Iterator<Header>() {
                        @Override
                        public boolean hasNext() {
                            return iterator.hasNext();
                        }

                        @Override
                        public Header next() {
                            return getHeader(iterator.next());
                        }
                    };
                }

                @Override
                public String toString() {
                    return response.headers().toString();
                }
            };
        }

        @Nonnull
        @Override
        public List<? extends cn.maarlakes.common.http.Cookie> getCookies() {
            return new ArrayList<>(this.cookies);
        }

        @Override
        public SocketAddress getRemoteAddress() {
            try {
                final HttpUrl url = this.response.request().url();
                final int port = url.port();
                return new InetSocketAddress(InetAddress.getByName(url.host()), port);
            } catch (Exception e) {
                return null;
            }
        }

    }
}
