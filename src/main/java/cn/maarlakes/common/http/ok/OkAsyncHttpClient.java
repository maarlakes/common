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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;

/**
 * 基于 OkHttp 的 {@link HttpClient} 异步适配实现。
 *
 * <p>OkHttp 本身是同步阻塞模型，本实现通过 {@link Call#enqueue(Callback)} 将网络 I/O
 * 放到 OkHttp 内部线程池执行，再以 {@link CompletableFuture} 对外暴露异步语义。
 *
 * <p>每次请求通过 {@link OkHttpClient#newBuilder()} 派生子客户端以应用请求级配置
 * （超时、代理、Cookie 等），避免修改共享的底层连接池和调度器。
 *
 * <p>本实现不是线程安全的——构造完成后各字段不可变，但底层 OkHttpClient 自身的连接池是线程安全的。
 *
 * @author linjpxc
 */
public class OkAsyncHttpClient implements HttpClient {

    private static final Logger log = LoggerFactory.getLogger(OkAsyncHttpClient.class);

    private final OkHttpClient client;
    private final SSLContext sslContext;
    private final RequestConfig defaultConfig;

    /**
     * 使用默认 OkHttpClient 构造，无 SSL 和默认配置。
     */
    public OkAsyncHttpClient() {
        this(new OkHttpClient.Builder().build(), null, null);
    }

    /**
     * 使用默认 OkHttpClient 构造，指定请求默认配置。
     *
     * @param defaultConfig 请求默认配置，可为 null
     */
    public OkAsyncHttpClient(RequestConfig defaultConfig) {
        this(new OkHttpClient.Builder().build(), null, defaultConfig);
    }

    /**
     * 使用外部提供的 OkHttpClient 构造。
     *
     * @param client OkHttpClient 实例，不允许为 null
     */
    public OkAsyncHttpClient(@Nonnull OkHttpClient client) {
        this(client, null, null);
    }

    /**
     * 使用外部提供的 OkHttpClient 构造，指定请求默认配置。
     *
     * @param client        OkHttpClient 实例，不允许为 null
     * @param defaultConfig 请求默认配置，可为 null
     */
    public OkAsyncHttpClient(@Nonnull OkHttpClient client, RequestConfig defaultConfig) {
        this(client, null, defaultConfig);
    }

    /**
     * 使用外部提供的 OkHttpClient 和 SSL 上下文构造。
     *
     * @param client      OkHttpClient 实例，不允许为 null
     * @param sslContext  SSL 上下文，用于覆盖默认 SSLSocketFactory，可为 null
     */
    public OkAsyncHttpClient(@Nonnull OkHttpClient client, SSLContext sslContext) {
        this(client, sslContext, null);
    }

    /**
     * 完整参数构造，所有其他构造方法均委托到此。
     *
     * @param client        OkHttpClient 实例，不允许为 null
     * @param sslContext    SSL 上下文，可为 null
     * @param defaultConfig 请求默认配置，可为 null
     */
    public OkAsyncHttpClient(@Nonnull OkHttpClient client, SSLContext sslContext, RequestConfig defaultConfig) {
        this.client = client;
        this.sslContext = sslContext;
        this.defaultConfig = defaultConfig;
        log.debug("OkAsyncHttpClient 已初始化，sslContext={}, defaultConfig={}", sslContext != null, defaultConfig != null);
    }

    /**
     * 将请求适配为 OkHttp {@link Call} 并异步执行，将完整响应体缓冲到内存后返回。
     *
     * <p>实现策略：构建 OkHttp {@link okhttp3.Request}，通过 {@link Call#enqueue(Callback)}
     * 提交到 OkHttp 内部调度器，在回调中完成 {@link CompletableFuture}。
     * 构建请求失败时返回一个异常已完成的 future，而非抛出同步异常。
     */
    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
        final String method = request.getMethod().name();
        final String uri = request.getUri().toString();
        log.debug("发送请求: {} {}", method, uri);
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
                    log.error("请求执行失败: {} {}", method, uri, e);
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }

                @Override
                public void onResponse(@Nonnull Call call, @Nonnull okhttp3.Response response) throws IOException {
                    log.debug("收到响应: {} {} -> {}", method, uri, response.code());
                    future.complete(new DefaultResponse(response, responseCookies));
                }
            });
            return future;
        } catch (Exception e) {
            log.error("构建 OkHttp 请求失败: {} {}", method, uri, e);
            return CompletableFuture.supplyAsync(() -> {
                throw new HttpClientException(e.getMessage(), e);
            });
        }
    }

    /**
     * 将请求适配为 OkHttp {@link Call} 并异步执行，通过 {@link ResponseHandler} 流式处理响应体。
     *
     * <p>实现策略：响应到达后将响应头信息（{@link HttpResponse}）和基于响应流的
     * {@link InputStreamBodySink} 交给 {@code handler}，由 handler 自行消费流数据。
     * 这样可以避免将整个响应体缓冲到内存，适合大文件等场景。
     */
    @Nonnull
    @Override
    public <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
        final String method = request.getMethod().name();
        final String uri = request.getUri().toString();
        log.debug("发送流式请求: {} {}", method, uri);
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
                    log.error("流式请求执行失败: {} {}", method, uri, e);
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }

                @Override
                public void onResponse(@Nonnull Call call, @Nonnull okhttp3.Response response) throws IOException {
                    log.debug("流式请求收到响应: {} {} -> {}", method, uri, response.code());
                    try {
                        final HttpResponse info = createResponseInfo(response, responseCookies);
                        // handler 负责消费底层 InputStream，不在此处缓冲整个响应体
                        final BodySink body = new InputStreamBodySink(response.body() != null ? response.body().byteStream() : null);
                        final CompletableFuture<T> result = handler.handle(info, body);
                        result.whenComplete((val, err) -> {
                            if (err != null) {
                                future.completeExceptionally(err);
                            } else {
                                future.complete(val);
                            }
                        });
                    } catch (Exception e) {
                        log.error("流式响应处理异常: {} {}", method, uri, e);
                        future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                    }
                }
            });
            return future;
        } catch (Exception e) {
            log.error("构建流式 OkHttp 请求失败: {} {}", method, uri, e);
            final CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(new HttpClientException(e.getMessage(), e));
            return future;
        }
    }

    /**
     * 基于共享的 OkHttpClient 派生子客户端，应用请求级配置和 Cookie 处理。
     *
     * <p>OkHttp 的 {@link OkHttpClient#newBuilder()} 会共享底层连接池和线程池，
     * 因此每次请求创建子实例的开销很小，且不会影响其他并发请求的配置。
     */
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
                    // 通过 SPI 加载所有 ProxyAuthenticator 实现，找到第一个能处理该代理认证的
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
            final TrustManagerFactory tmf;
            try {
                tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
                tmf.init((java.security.KeyStore) null);
                X509TrustManager trustManager = null;
                for (TrustManager tm : tmf.getTrustManagers()) {
                    if (tm instanceof X509TrustManager) {
                        trustManager = (X509TrustManager) tm;
                        break;
                    }
                }
                if (trustManager != null) {
                    builder.sslSocketFactory(this.sslContext.getSocketFactory(), trustManager);
                }
            } catch (Exception ignored) {
                // 回退到弃用 API，总比没有 SSL 好
                builder.sslSocketFactory(this.sslContext.getSocketFactory());
            }
        }
        return builder.build();
    }

    /**
     * 将统一请求模型中的请求体转换为 OkHttp 的 {@link RequestBody}。
     *
     * <p>处理顺序：multipart body -> 普通 body -> form 参数 -> null（GET 等无 body 请求）。
     */
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

    /**
     * 关闭客户端。OkHttp 的 OkHttpClient 资源由其内部连接池管理，
     * 无需显式关闭，因此本实现为空操作。
     */
    @Override
    public void close() {
        log.debug("OkAsyncHttpClient close 调用（OkHttpClient 无需显式关闭）");
    }

    /**
     * 支持取消底层 OkHttp {@link Call} 的 {@link CompletableFuture} 实现。
     */
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

    /**
     * 支持取消底层 OkHttp {@link Call} 的泛型 {@link CompletableFuture} 实现，用于流式处理场景。
     */
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

    /**
     * 从 OkHttp 响应中提取响应头信息，构造框架统一的 {@link HttpResponse}。
     */
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

    /**
     * 将 OkHttp 响应适配为框架统一的 {@link Response}。
     *
     * <p>构造时即将响应体完整读取为字节数组并关闭底层响应，避免资源泄漏。
     */
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
                // 无 body 时立即关闭响应，释放连接
                response.close();
            } else {
                try {
                    // 一次性读取全部 body 字节后关闭，避免连接泄漏
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
