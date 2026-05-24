package cn.maarlakes.common.http.async;

import cn.maarlakes.common.http.*;
import cn.maarlakes.common.http.Request;
import cn.maarlakes.common.http.Response;
import cn.maarlakes.common.http.body.multipart.FilePart;
import cn.maarlakes.common.http.body.multipart.MultipartBody;
import cn.maarlakes.common.http.body.multipart.MultipartPart;
import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.CollectionUtils;
import io.netty.handler.codec.http.cookie.CookieHeaderNames;
import jakarta.annotation.Nonnull;
import org.asynchttpclient.*;
import org.asynchttpclient.netty.ssl.JsseSslEngineFactory;
import org.asynchttpclient.proxy.ProxyServer;
import org.asynchttpclient.request.body.multipart.InputStreamPart;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 基于 AsyncHttpClient（Netty）的 {@link HttpClient} 异步实现。
 *
 * <p>AsyncHttpClient 是真正的异步 HTTP 客户端，底层基于 Netty 事件循环驱动网络 I/O，
 * 天然适配 {@link CompletableFuture} 的异步模型。本实现将其 API 映射到框架统一的
 * {@link HttpClient} 接口。
 *
 * <p>两个 {@code execute} 重载分别对应两种消费模式：
 * <ul>
 *   <li>无 handler 版本：通过 {@link AsyncHttpClient#executeRequest(RequestBuilder)}
 *       直接获得完整响应，适配简单请求场景</li>
 *   <li>有 handler 版本：通过 {@link AsyncCompletionHandler} 的回调逐步接收响应头和 body chunk，
 *       将流数据推送给 {@link ResponseHandler}，避免缓冲完整响应体</li>
 * </ul>
 *
 * <p>本实现不是线程安全的——构造完成后各字段不可变，底层 AsyncHttpClient 自身是线程安全的。
 *
 * @author linjpxc
 */
public class NettyAsyncHttpClient implements HttpClient {

    private static final Logger log = LoggerFactory.getLogger(NettyAsyncHttpClient.class);

    private final AsyncHttpClient client;
    private final RequestConfig defaultConfig;

    /**
     * 使用默认 AsyncHttpClient 配置构造。
     */
    public NettyAsyncHttpClient() {
        this(Dsl.asyncHttpClient(), null);
    }

    /**
     * 使用默认 AsyncHttpClient 配置构造，指定请求默认配置。
     *
     * @param defaultConfig 请求默认配置，可为 null
     */
    public NettyAsyncHttpClient(RequestConfig defaultConfig) {
        this(Dsl.asyncHttpClient(), defaultConfig);
    }

    /**
     * 使用指定 SSL 上下文构造，覆盖默认 SSL 引擎工厂。
     *
     * @param sslContext SSL 上下文，不允许为 null
     */
    public NettyAsyncHttpClient(@Nonnull SSLContext sslContext) {
        this(new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setSslEngineFactory(new JsseSslEngineFactory(sslContext))
                        .build()
        ), null);
    }

    /**
     * 使用指定 SSL 上下文构造，并指定请求默认配置。
     *
     * @param sslContext    SSL 上下文，不允许为 null
     * @param defaultConfig 请求默认配置，可为 null
     */
    public NettyAsyncHttpClient(@Nonnull SSLContext sslContext, RequestConfig defaultConfig) {
        this(new DefaultAsyncHttpClient(
                new DefaultAsyncHttpClientConfig.Builder()
                        .setSslEngineFactory(new JsseSslEngineFactory(sslContext))
                        .build()
        ), defaultConfig);
    }

    /**
     * 使用外部提供的 AsyncHttpClient 构造。
     *
     * @param client AsyncHttpClient 实例，不允许为 null
     */
    public NettyAsyncHttpClient(@Nonnull AsyncHttpClient client) {
        this(client, null);
    }

    /**
     * 完整参数构造，所有其他构造方法均委托到此。
     *
     * @param client        AsyncHttpClient 实例，不允许为 null
     * @param defaultConfig 请求默认配置，可为 null
     */
    public NettyAsyncHttpClient(@Nonnull AsyncHttpClient client, RequestConfig defaultConfig) {
        this.client = client;
        this.defaultConfig = defaultConfig;
        log.debug("NettyAsyncHttpClient 已初始化，defaultConfig={}", defaultConfig != null);
    }

    /**
     * 将请求适配为 AsyncHttpClient 的 {@link RequestBuilder} 并执行，将完整响应缓冲后返回。
     *
     * <p>实现策略：使用 {@link AsyncHttpClient#executeRequest(RequestBuilder)} 的
     * {@code ListenableFuture} 转为 {@link CompletableFuture}，异常统一包装为
     * {@link HttpClientException}，正常响应通过 {@link DefaultResponse} 适配。
     */
    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
        final String method = request.getMethod().name();
        final String uri = request.getUri().toString();
        log.debug("发送请求: {} {}", method, uri);
        return this.client.executeRequest(toBuilder(request, effectiveConfig))
                .toCompletableFuture()
                .exceptionally(error -> {
                    log.error("请求执行失败: {} {}", method, uri, error);
                    throw new HttpClientException(error.getMessage(), error);
                })
                .thenApply(response -> {
                    log.debug("收到响应: {} {} -> {}", method, uri, response.getStatusCode());
                    return new DefaultResponse(response);
                });
    }

    /**
     * 将请求适配为 AsyncHttpClient 的 {@link RequestBuilder} 并执行，通过
     * {@link AsyncCompletionHandler} 回调逐步推送响应数据给 {@link ResponseHandler}。
     *
     * <p>实现策略：
     * <ol>
     *   <li>收到响应头时立即调用 handler（通过 {@code invokeHandler}），传入 {@link PushBodySink}</li>
     *   <li>后续 body chunk 通过 {@link PushBodySink#pushChunk} 实时推送给 handler</li>
     *   <li>响应完成时调用 {@link PushBodySink#complete()} 通知 handler 流结束</li>
     * </ol>
     *
     * <p>使用 {@code AtomicBoolean} 确保 handler 只被调用一次，防止在 onHeadersReceived
     * 和 onCompleted 中重复触发。{@code result} future 支持通过取消来中断底层的
     * {@link ListenableFuture}。
     */
    @Nonnull
    @Override
    public <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
        final String method = request.getMethod().name();
        final String uri = request.getUri().toString();
        log.debug("发送流式请求: {} {}", method, uri);
        final AtomicReference<ListenableFuture<Void>> listenableFutureRef = new AtomicReference<>();
        final CompletableFuture<T> result = new CompletableFuture<T>() {
            @Override
            public boolean cancel(boolean mayInterruptIfRunning) {
                if (mayInterruptIfRunning) {
                    final ListenableFuture<Void> lf = listenableFutureRef.get();
                    if (lf != null) {
                        lf.cancel(true);
                    }
                }
                return super.cancel(mayInterruptIfRunning);
            }
        };
        final cn.maarlakes.common.http.PushBodySink sink = new cn.maarlakes.common.http.PushBodySink();
        final ListenableFuture<Void> listenableFuture = this.client.executeRequest(
                toBuilder(request, effectiveConfig),
                new AsyncCompletionHandler<Void>() {
                    private HttpResponseStatus status;
                    private io.netty.handler.codec.http.HttpHeaders headers;
                    // 确保 invokeHandler 只执行一次，防止 onHeadersReceived 和 onCompleted 重复触发
                    private final AtomicBoolean handlerInvoked = new AtomicBoolean();

                    @Override
                    public State onStatusReceived(HttpResponseStatus status) throws Exception {
                        this.status = status;
                        return State.CONTINUE;
                    }

                    @Override
                    public State onHeadersReceived(io.netty.handler.codec.http.HttpHeaders headers) throws Exception {
                        this.headers = headers;
                        super.onHeadersReceived(headers);
                        // 收到完整头部后立即触发 handler，使 handler 可以在 body 到达前开始工作
                        invokeHandler();
                        return State.CONTINUE;
                    }

                    @Override
                    public State onBodyPartReceived(HttpResponseBodyPart part) throws Exception {
                        sink.pushChunk(part.getBodyPartBytes(), 0, part.length());
                        return State.CONTINUE;
                    }

                    @Override
                    public Void onCompleted(org.asynchttpclient.Response response) throws Exception {
                        // 若未收到 headers 回调（理论上不会发生），在此兜底触发 handler
                        if (!handlerInvoked.get()) {
                            invokeHandler();
                        }
                        sink.complete();
                        return null;
                    }

                    @Override
                    public void onThrowable(Throwable t) {
                        log.error("流式请求执行异常: {} {}", method, uri, t);
                        if (handlerInvoked.get()) {
                            // handler 已被调用，通过 sink 传递异常让 handler 自行处理
                            sink.fail(t);
                        } else {
                            result.completeExceptionally(new HttpClientException(t.getMessage(), t));
                        }
                    }

                    private void invokeHandler() {
                        if (handlerInvoked.compareAndSet(false, true)) {
                            log.debug("流式请求收到响应头: {} {} -> {}", method, uri, status.getStatusCode());
                            final HttpResponse httpResponse = createResponseInfo(status, headers, request.getUri());
                            final CompletableFuture<T> handlerFuture = handler.handle(httpResponse, sink);
                            handlerFuture.whenComplete((val, err) -> {
                                if (err != null) {
                                    result.completeExceptionally(err);
                                } else {
                                    result.complete(val);
                                }
                            });
                        }
                    }
                }
        );
        listenableFutureRef.set(listenableFuture);
        listenableFuture.toCompletableFuture().exceptionally(ex -> {
            if (!result.isDone()) {
                result.completeExceptionally(ex);
            }
            return null;
        });
        return result;
    }

    /**
     * 将框架统一请求模型转换为 AsyncHttpClient 的 {@link RequestBuilder}，应用请求级配置。
     */
    private static RequestBuilder toBuilder(@Nonnull Request request, RequestConfig config) {
        final RequestBuilder builder = toBuilder(request.getMethod(), request.getUri().toString());
        if (!request.getHeaders().isEmpty()) {
            for (Header header : request.getHeaders()) {
                builder.addHeader(header.getName(), header.getValues());
            }
        }
        if (CollectionUtils.isNotEmpty(request.getCookies())) {
            for (Cookie cookie : request.getCookies()) {
                builder.addCookie(toCookie(cookie));
            }
        }
        if (request.getCharset() != null) {
            builder.setCharset(request.getCharset());
        }
        if (CollectionUtils.isNotEmpty(request.getQueryParams())) {
            for (NameValuePair param : request.getQueryParams()) {
                builder.addQueryParam(param.getName(), param.getValue());
            }
        }
        if (CollectionUtils.isNotEmpty(request.getFormParams())) {
            for (NameValuePair param : request.getFormParams()) {
                builder.addFormParam(param.getName(), param.getValue());
            }
        }

        if (request.getBody() != null) {
            if (request.getBody() instanceof MultipartBody) {
                final MultipartBody multipartBody = (MultipartBody) request.getBody();
                if (CollectionUtils.isNotEmpty(multipartBody.getContent())) {
                    for (MultipartPart<?> part : multipartBody.getContent()) {
                        final Header contentId = part.getHeaders().getHeader("Content-ID");
                        final Header contentTransferEncoding = part.getHeaders().getHeader("Content-Transfer-Encoding");
                        if (part instanceof FilePart) {
                            builder.addBodyPart(new org.asynchttpclient.request.body.multipart.FilePart(
                                    part.getName(), ((FilePart) part).getContent(), part.getContentType() == null ? null : ContentTypes.toString(part.getContentType()),
                                    part.getCharset(), part.getFilename(), contentId == null ? null : contentId.get(),
                                    contentTransferEncoding == null ? null : contentTransferEncoding.get()));
                        } else {
                            builder.addBodyPart(new InputStreamPart(
                                    part.getName(), part.getContentStream(), part.getFilename(), -1,
                                    part.getContentType() == null ? null : ContentTypes.toString(part.getContentType()),
                                    part.getCharset(), contentId == null ? null : contentId.get(),
                                    contentTransferEncoding == null ? null : contentTransferEncoding.get()));
                        }
                    }
                }
            } else {
                builder.setBody(request.getBody().getContentStream());
                final Header contentTypeHeader = request.getBody().getContentTypeHeader();
                if (contentTypeHeader != null) {
                    builder.setHeader(contentTypeHeader.getName(), contentTypeHeader.getValues());
                }
            }
        }
        if (config != null) {
            if (config.isRedirectsEnabled() != null) {
                builder.setFollowRedirect(config.isRedirectsEnabled());
            }
            if (config.getResponseTimeout() != null) {
                builder.setReadTimeout((int) config.getResponseTimeout().toMillis());
            }
            if (config.getRequestTimeout() != null) {
                builder.setRequestTimeout((int) config.getRequestTimeout().toMillis());
            }
            if (config.getProxy() != null) {
                if (config.getProxyAuthentication() == null) {
                    final InetSocketAddress address = (InetSocketAddress) config.getProxy().address();
                    builder.setProxyServer(new ProxyServer.Builder(address.getHostName(), address.getPort()));
                } else {
                    // 通过 SPI 加载所有 ProxyAuthenticator 实现，找到第一个能处理该代理认证的
                    for (ProxyAuthenticator authenticator : SpiServiceLoader.loadShared(ProxyAuthenticator.class, NettyAsyncHttpClient.class.getClassLoader())) {
                        final ProxyServer.Builder proxyServerBuilder = authenticator.authenticate(config.getProxy(), config.getProxyAuthentication());
                        if (proxyServerBuilder != null) {
                            builder.setProxyServer(proxyServerBuilder);
                            break;
                        }
                    }
                }
            }
        }
        return builder;
    }

    /**
     * 将框架统一 Cookie 转换为 Netty 的 {@link io.netty.handler.codec.http.cookie.Cookie}。
     */
    @Nonnull
    private static io.netty.handler.codec.http.cookie.Cookie toCookie(@Nonnull Cookie cookie) {
        final io.netty.handler.codec.http.cookie.DefaultCookie defaultCookie = new io.netty.handler.codec.http.cookie.DefaultCookie(cookie.name(), cookie.value());
        defaultCookie.setDomain(cookie.domain());
        defaultCookie.setPath(cookie.path());
        defaultCookie.setMaxAge(cookie.maxAge());
        defaultCookie.setSecure(cookie.isSecure());
        defaultCookie.setHttpOnly(cookie.isHttpOnly());
        if (cookie.sameSite() != null) {
            defaultCookie.setSameSite(CookieHeaderNames.SameSite.valueOf(cookie.sameSite().name()));
        }
        return defaultCookie;
    }

    /**
     * 从 AsyncHttpClient 的响应状态和头部构造框架统一的 {@link HttpResponse}。
     */
    private static HttpResponse createResponseInfo(HttpResponseStatus status, io.netty.handler.codec.http.HttpHeaders headers, URI uri) {
        final HttpHeaders httpHeaders = new AsyncHttpHeaders(headers);
        return new DefaultHttpResponse(
                status.getStatusCode(),
                status.getStatusText(),
                httpHeaders,
                uri,
                Cookies.parseFromHeaders(httpHeaders),
                status.getRemoteAddress()
        );
    }


    private static RequestBuilder toBuilder(@Nonnull HttpMethod method, @Nonnull String url) {
        return Dsl.request(method.name(), url);
    }

    /**
     * 关闭底层 AsyncHttpClient，释放 Netty 资源（事件循环、连接池等）。
     *
     * <p>关闭异常被静默处理，因为 AsyncHttpClient 关闭时可能抛出已中断的 channel 异常。
     */
    @Override
    public void close() {
        log.debug("关闭 NettyAsyncHttpClient");
        try {
            this.client.close();
        } catch (Exception ignored) {
            // AsyncHttpClient 关闭时可能抛出 channel 已关闭等异常，属于正常关闭场景
            log.debug("关闭 AsyncHttpClient 时忽略异常: {}", ignored.getMessage());
        }
    }

    /**
     * 将 AsyncHttpClient 响应适配为框架统一的 {@link Response}。
     *
     * <p>响应体使用双重检查锁定的懒加载模式，首次访问时才读取字节数组。
     */
    protected static class DefaultResponse implements Response {

        private final org.asynchttpclient.Response response;
        private volatile ResponseBody body;

        protected DefaultResponse(org.asynchttpclient.Response response) {
            this.response = response;
        }

        @Override
        public int getStatusCode() {
            return this.response.getStatusCode();
        }

        @Override
        public String getStatusText() {
            return this.response.getStatusText();
        }

        @Nonnull
        @Override
        public ResponseBody getBody() {
            // 双重检查锁定：延迟读取响应体字节，避免在构造时即占用内存
            if (this.body == null) {
                synchronized (this) {
                    if (this.body == null) {
                        this.body = new ByteArrayResponseBody(
                                this.response.getResponseBodyAsBytes(),
                                Optional.ofNullable(this.response.getContentType()).map(ContentType::parse).orElse(null),
                                this.getHeaders().getHeader(HttpHeaderNames.CONTENT_ENCODING)
                        );
                    }
                }
            }
            return this.body;
        }

        @Override
        public URI getUri() {
            try {
                return new URI(this.response.getUri().toString());
            } catch (URISyntaxException e) {
                throw new IllegalArgumentException(e);
            }
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            return new AsyncHttpHeaders(this.response.getHeaders());
        }

        @Nonnull
        @Override
        public List<? extends Cookie> getCookies() {
            final List<io.netty.handler.codec.http.cookie.Cookie> cookies = this.response.getCookies();
            if (CollectionUtils.isEmpty(cookies)) {
                return Collections.emptyList();
            }
            final List<Cookie> list = new ArrayList<>();
            for (io.netty.handler.codec.http.cookie.Cookie cookie : cookies) {
                final Cookie.Builder builder = Cookie.builder(cookie.name())
                        .value(cookie.value())
                        .domain(cookie.domain())
                        .path(cookie.path())
                        .maxAge(cookie.maxAge())
                        .isSecure(cookie.isSecure())
                        .isHttpOnly(cookie.isHttpOnly());
                if (cookie instanceof io.netty.handler.codec.http.cookie.DefaultCookie) {
                    final CookieHeaderNames.SameSite sameSite = ((io.netty.handler.codec.http.cookie.DefaultCookie) cookie).sameSite();
                    if (sameSite != null) {
                        builder.sameSite(Cookie.SameSite.of(sameSite.name()));
                    }
                }
                list.add(builder.build());
            }
            return list;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return this.response.getRemoteAddress();
        }

        @Override
        public String toString() {
            return this.response.toString();
        }
    }

    /**
     * 将 Netty 的 {@link io.netty.handler.codec.http.HttpHeaders} 适配为框架统一的 {@link HttpHeaders}。
     */
    private static final class AsyncHttpHeaders implements HttpHeaders {

        private final io.netty.handler.codec.http.HttpHeaders headers;

        private AsyncHttpHeaders(@Nonnull io.netty.handler.codec.http.HttpHeaders headers) {
            this.headers = headers;
        }

        @Override
        public boolean isEmpty() {
            return this.headers.isEmpty();
        }

        @Override
        public Header getHeader(@Nonnull String name) {
            final List<String> list = this.headers.getAll(name);
            return new DefaultHeader(name, list == null ? new ArrayList<>() : list);
        }

        @Nonnull
        @Override
        public Iterator<Header> iterator() {
            final Iterator<String> iterator = this.headers.names().iterator();
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
            return this.headers.toString();
        }
    }
}
