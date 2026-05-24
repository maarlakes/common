package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.http.*;
import cn.maarlakes.common.http.ContentType;
import cn.maarlakes.common.http.Header;
import cn.maarlakes.common.http.HttpHeaders;
import cn.maarlakes.common.http.NameValuePair;
import cn.maarlakes.common.http.body.multipart.FilePart;
import cn.maarlakes.common.http.body.multipart.MultipartBody;
import cn.maarlakes.common.http.body.multipart.MultipartPart;
import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.CollectionUtils;
import jakarta.annotation.Nonnull;
import org.apache.hc.client5.http.async.HttpAsyncClient;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.cookie.Cookie;
import org.apache.hc.client5.http.cookie.CookieStore;
import org.apache.hc.client5.http.entity.mime.FileBody;
import org.apache.hc.client5.http.entity.mime.FormBodyPartBuilder;
import org.apache.hc.client5.http.entity.mime.InputStreamBody;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.impl.async.CloseableHttpAsyncClient;
import org.apache.hc.client5.http.impl.async.HttpAsyncClientBuilder;
import org.apache.hc.client5.http.impl.async.HttpAsyncClients;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.core5.concurrent.FutureCallback;
import org.apache.hc.core5.http.*;
import org.apache.hc.core5.http.HttpResponse;
import org.apache.hc.core5.http.nio.AsyncResponseConsumer;
import org.apache.hc.core5.http.nio.CapacityChannel;
import org.apache.hc.core5.http.nio.entity.BasicAsyncEntityConsumer;
import org.apache.hc.core5.http.nio.ssl.BasicClientTlsStrategy;
import org.apache.hc.core5.http.nio.support.AbstractAsyncResponseConsumer;
import org.apache.hc.core5.http.nio.support.AsyncRequestBuilder;
import org.apache.hc.core5.http.protocol.HttpContext;
import org.apache.hc.core5.reactor.IOReactorStatus;
import org.apache.hc.core5.util.Timeout;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

/**
 * 基于 Apache HttpClient 5.x 异步 API 的 {@link HttpClient} 适配实现。
 *
 * <p>将统一的 {@link HttpClient} 异步契约适配到 Apache HC 5 的
 * {@link HttpAsyncClient} 非阻塞模型。所有请求均通过 Apache 的 NIO 反应器执行，
 * 不会占用调用线程进行 I/O 等待。
 *
 * <p>构造时若传入的 {@link HttpAsyncClient} 是 {@link CloseableHttpAsyncClient} 且
 * 未启动（{@link IOReactorStatus#ACTIVE}），会自动调用 {@code start()}。
 *
 * <p>线程安全性：委托给底层 Apache {@link HttpAsyncClient}，其自身是线程安全的，
 * 本实例可在多线程间共享。
 *
 * @author linjpxc
 */
public class ApacheAsyncHttpClient implements HttpClient {

    private static final Logger log = LoggerFactory.getLogger(ApacheAsyncHttpClient.class);

    private final HttpAsyncClient client;
    private final RequestConfig defaultConfig;

    /**
     * 使用默认 HttpAsyncClient 构建，无自定义配置。
     */
    public ApacheAsyncHttpClient() {
        this(HttpAsyncClientBuilder.create().build(), null);
    }

    /**
     * 使用指定 SSL 上下文构建异步客户端，用于 HTTPS 通信。
     *
     * @param sslContext SSL/TLS 上下文，不允许为 null
     */
    public ApacheAsyncHttpClient(@Nonnull SSLContext sslContext) {
        this(buildClient(sslContext), null);
    }

    /**
     * 使用指定 SSL 上下文和默认请求配置构建。
     *
     * @param sslContext    SSL/TLS 上下文，不允许为 null
     * @param defaultConfig 默认请求配置，可为 null
     */
    public ApacheAsyncHttpClient(@Nonnull SSLContext sslContext, RequestConfig defaultConfig) {
        this(buildClient(sslContext), defaultConfig);
    }

    /**
     * 使用外部提供的 {@link HttpAsyncClient} 实例，无默认配置。
     *
     * @param httpClient 已配置的 Apache 异步 HTTP 客户端，不允许为 null
     */
    public ApacheAsyncHttpClient(@Nonnull HttpAsyncClient httpClient) {
        this(httpClient, null);
    }

    /**
     * 使用外部提供的 {@link HttpAsyncClient} 实例和默认请求配置。
     *
     * <p>若传入的客户端是 {@link CloseableHttpAsyncClient} 且尚未启动，会自动调用
     * {@code start()} 以初始化底层反应器。
     *
     * @param httpClient    已配置的 Apache 异步 HTTP 客户端，不允许为 null
     * @param defaultConfig 默认请求配置，可为 null
     */
    public ApacheAsyncHttpClient(@Nonnull HttpAsyncClient httpClient, RequestConfig defaultConfig) {
        this.client = httpClient;
        this.defaultConfig = defaultConfig;
        if (httpClient instanceof CloseableHttpAsyncClient) {
            final CloseableHttpAsyncClient closeableHttpAsyncClient = (CloseableHttpAsyncClient) httpClient;
            if (closeableHttpAsyncClient.getStatus() != IOReactorStatus.ACTIVE) {
                log.debug("自动启动 CloseableHttpAsyncClient (当前状态: {})", closeableHttpAsyncClient.getStatus());
                closeableHttpAsyncClient.start();
            }
        }
    }

    /**
     * 发送请求并完整缓冲响应体到内存。
     *
     * <p>实现策略：将 {@link Request} 转换为 Apache HC 5 的异步请求生产者，
     * 通过 {@link ResponseAsyncResponseConsumer} 将整个响应体读入字节数组后构建 {@link Response}。
     *
     * @param request HTTP 请求，不允许为 null
     * @param config  请求级配置，可为 null（使用客户端默认配置）
     * @return 异步响应，完成后包含完整响应
     */
    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
        log.debug("发送异步请求: {} {}", request.getMethod(), request.getUri());
        try {
            final PreparedRequest prepared = prepareRequest(request, effectiveConfig);
            final ResponseFuture future = new ResponseFuture();
            future.future = this.client.execute(prepared.request, new ResponseAsyncResponseConsumer(request.getUri(), prepared.context), null, prepared.context, new FutureCallback<Response>() {
                @Override
                public void completed(Response response) {
                    log.debug("收到异步响应: {} {} -> {}", request.getMethod(), request.getUri(), response.getStatusCode());
                    future.complete(response);
                }

                @Override
                public void failed(Exception ex) {
                    log.error("异步请求失败: {} {} - {}", request.getMethod(), request.getUri(), ex.getMessage(), ex);
                    future.completeExceptionally(new HttpClientException(ex.getMessage(), ex));
                }

                @Override
                public void cancelled() {
                    log.warn("异步请求被取消: {} {}", request.getMethod(), request.getUri());
                    future.cancel(false);
                }
            });

            return future;
        } catch (Exception e) {
            log.error("准备异步请求失败: {} {} - {}", request.getMethod(), request.getUri(), e.getMessage(), e);
            final CompletableFuture<Response> future = new CompletableFuture<>();
            future.completeExceptionally(new HttpClientException(e.getMessage(), e));
            return future;
        }
    }

    /**
     * 发送请求并通过 {@link ResponseHandler} 流式处理响应体。
     *
     * <p>实现策略：使用自定义的 {@link StreamingResponseConsumer} 将响应块逐块推送到
     * {@link cn.maarlakes.common.http.PushBodySink}，由 handler 实时消费，避免将完整响应体
     * 缓冲到内存。适合处理大文件下载或流式数据。
     *
     * @param request HTTP 请求，不允许为 null
     * @param config  请求级配置，可为 null
     * @param handler 响应流处理器，不允许为 null
     * @param <T>     handler 的返回类型
     * @return 异步结果，完成后包含 handler 的处理结果
     */
    @Nonnull
    @Override
    public <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
        log.debug("发送流式异步请求: {} {}", request.getMethod(), request.getUri());
        try {
            final PreparedRequest prepared = prepareRequest(request, effectiveConfig);
            final HandlerFuture<T> future = new HandlerFuture<>();
            future.future = this.client.execute(prepared.request, new StreamingResponseConsumer<>(request, handler), null, prepared.context, new FutureCallback<T>() {
                @Override
                public void completed(T response) {
                    log.debug("流式异步请求完成: {} {}", request.getMethod(), request.getUri());
                    future.complete(response);
                }

                @Override
                public void failed(Exception ex) {
                    log.error("流式异步请求失败: {} {} - {}", request.getMethod(), request.getUri(), ex.getMessage(), ex);
                    future.completeExceptionally(new HttpClientException(ex.getMessage(), ex));
                }

                @Override
                public void cancelled() {
                    log.warn("流式异步请求被取消: {} {}", request.getMethod(), request.getUri());
                    future.cancel(false);
                }
            });

            return future;
        } catch (Exception e) {
            log.error("准备流式异步请求失败: {} {} - {}", request.getMethod(), request.getUri(), e.getMessage(), e);
            final CompletableFuture<T> future = new CompletableFuture<>();
            future.completeExceptionally(new HttpClientException(e.getMessage(), e));
            return future;
        }
    }

    /**
     * 关闭底层 Apache 异步 HTTP 客户端，释放连接池和反应器资源。
     *
     * <p>仅当底层客户端实现了 {@link AutoCloseable} 时才执行关闭操作。
     * 关闭后不应再发起请求。
     */
    @Override
    public void close() {
        if (this.client instanceof AutoCloseable) {
            try {
                log.debug("关闭 Apache HttpAsyncClient");
                ((AutoCloseable) this.client).close();
            } catch (Exception e) {
                log.error("关闭 Apache HttpAsyncClient 失败: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 将通用 {@link RequestConfig} 转换为 Apache HC 5 的 {@link org.apache.hc.client5.http.config.RequestConfig}。
     * 仅转换非 null 的配置项，null 值使用 Apache 默认值。
     */
    @SuppressWarnings("DuplicatedCode")
    private static org.apache.hc.client5.http.config.RequestConfig to(RequestConfig config) {
        if (config == null) {
            return null;
        }
        final org.apache.hc.client5.http.config.RequestConfig.Builder builder = org.apache.hc.client5.http.config.RequestConfig.custom();
        if (config.isRedirectsEnabled() != null) {
            builder.setRedirectsEnabled(config.isRedirectsEnabled());
        }
        if (config.getRequestTimeout() != null) {
            builder.setConnectionRequestTimeout(Timeout.ofMilliseconds(config.getRequestTimeout().toMillis()));
        }
        if (config.getResponseTimeout() != null) {
            builder.setResponseTimeout(Timeout.ofMilliseconds(config.getResponseTimeout().toMillis()));
        }
        if (config.getConnectTimeout() != null) {
            builder.setConnectTimeout(Timeout.ofMilliseconds(config.getConnectTimeout().toMillis()));
        }
        if (config.getProxy() != null) {
            final InetSocketAddress address = (InetSocketAddress) config.getProxy().address();
            builder.setProxy(new HttpHost(address.getAddress(), address.getPort()));
        }
        if (config.getMaxRedirects() != null && config.getMaxRedirects() > 0) {
            builder.setMaxRedirects(config.getMaxRedirects());
        }
        return builder.build();
    }

    /**
     * 封装 Apache HC 5 的异步请求生产者和 HTTP 上下文。
     */
    private static final class PreparedRequest {
        final org.apache.hc.core5.http.nio.AsyncRequestProducer request;
        final HttpClientContext context;

        PreparedRequest(org.apache.hc.core5.http.nio.AsyncRequestProducer request, HttpClientContext context) {
            this.request = request;
            this.context = context;
        }
    }

    /**
     * 将通用 {@link Request} 转换为 Apache HC 5 的异步请求生产者。
     *
     * <p>处理顺序：URI -> 请求头 -> 字符集 -> 表单参数 -> 请求体 -> Cookie -> 代理认证。
     * 每次调用创建独立的 {@link HttpClientContext} 和 {@link BasicCookieStore}，
     * 确保请求间 cookie 隔离。
     */
    private PreparedRequest prepareRequest(@Nonnull Request request, RequestConfig effectiveConfig) throws Exception {
        log.trace("准备异步请求: {} {}", request.getMethod(), request.getUri());
        final AsyncRequestBuilder builder = AsyncRequestBuilder.create(request.getMethod().name())
                .setUri(Apaches.toUri(request));
        settingHeader(builder, request);
        if (request.getCharset() != null) {
            builder.setCharset(request.getCharset());
        }
        settingFormParams(builder, request);
        if (request.getBody() != null) {
            if (request.getBody() instanceof MultipartBody) {
                settingMultipart(builder, (MultipartBody) request.getBody(), request.getCharset());
            } else {
                builder.setEntity(new ContentAsyncEntityProducer(request.getBody()));
            }
        }

        final HttpClientContext context = HttpClientContext.create();
        // 每次请求使用独立的 CookieStore，避免并发场景下的 cookie 串扰
        context.setCookieStore(new BasicCookieStore());
        final org.apache.hc.client5.http.config.RequestConfig requestConfig = to(effectiveConfig);
        if (requestConfig != null) {
            context.setRequestConfig(requestConfig);
        }
        if (effectiveConfig != null && effectiveConfig.getProxy() != null && effectiveConfig.getProxyAuthentication() != null) {
            for (ProxyAuthenticator authenticator : SpiServiceLoader.loadShared(ProxyAuthenticator.class, this.getClass().getClassLoader())) {
                if (authenticator.supported(effectiveConfig.getProxy(), effectiveConfig.getProxyAuthentication())) {
                    authenticator.authenticate(context, effectiveConfig.getProxy(), effectiveConfig.getProxyAuthentication());
                    break;
                }
            }
        }

        if (CollectionUtils.isNotEmpty(request.getCookies())) {
            builder.addHeader("Cookie", request.getCookies().stream().map(item -> item.name() + "=" + item.value()).collect(Collectors.joining(";")));
        }

        return new PreparedRequest(builder.build(), context);
    }

    /**
     * 设置表单参数到请求构建器。
     */
    private static void settingFormParams(@Nonnull AsyncRequestBuilder builder, @Nonnull Request request) {
        if (CollectionUtils.isNotEmpty(request.getFormParams())) {
            for (NameValuePair param : request.getFormParams()) {
                builder.addParameter(param.getName(), param.getValue());
            }
        }
    }

    /**
     * 设置请求头到请求构建器，支持同名头多值。
     */
    private static void settingHeader(@Nonnull AsyncRequestBuilder builder, @Nonnull Request request) {
        if (!request.getHeaders().isEmpty()) {
            for (Header header : request.getHeaders()) {
                for (String value : header.getValues()) {
                    builder.addHeader(header.getName(), value);
                }
            }
        }
    }

    /**
     * 构建 multipart/form-data 请求体，处理 FilePart 和普通 Part 两种类型。
     * FilePart 使用 {@link FileBody}，其他类型使用 {@link InputStreamBody}。
     */
    @SuppressWarnings("DuplicatedCode")
    private static void settingMultipart(@Nonnull AsyncRequestBuilder builder, @Nonnull MultipartBody body, Charset charset) {
        if (CollectionUtils.isNotEmpty(body.getContent())) {
            final MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            multipartEntityBuilder.setContentType(Apaches.toApacheContentType(Objects.requireNonNull(body.getContentType())));
            for (MultipartPart<?> part : body.getContent()) {
                org.apache.hc.client5.http.entity.mime.ContentBody contentBody;
                if (part instanceof FilePart) {
                    if (part.getContentType() == null) {
                        contentBody = new FileBody(((FilePart) part).getContent());
                    } else {
                        contentBody = new FileBody(((FilePart) part).getContent(), Apaches.toApacheContentType(part.getContentType()), part.getFilename());
                    }
                } else {
                    if (part.getContentType() == null) {
                        contentBody = new InputStreamBody(part.getContentStream(), part.getFilename());
                    } else {
                        contentBody = new InputStreamBody(part.getContentStream(), Apaches.toApacheContentType(part.getContentType()), part.getFilename());
                    }
                }
                final FormBodyPartBuilder partBuilder = FormBodyPartBuilder.create(part.getName(), contentBody);
                if (!part.getHeaders().isEmpty()) {
                    for (Header header : part.getHeaders()) {
                        for (String s : header.getValues()) {
                            if (s != null) {
                                partBuilder.addField(header.getName(), s);
                            }
                        }
                    }
                }
                multipartEntityBuilder.addPart(partBuilder.build());
            }
            if (charset != null) {
                multipartEntityBuilder.setCharset(charset);
            }
            builder.setEntity(new HttpEntityAsyncEntityProducer(multipartEntityBuilder.build()));
        }
    }

    /**
     * 将完整响应体读入字节数组的异步响应消费者。
     * 基于 Apache HC 5 的 {@link AbstractAsyncResponseConsumer}，先通过
     * {@link BasicAsyncEntityConsumer} 收集全部字节，再构建 {@link Response}。
     */
    private static class ResponseAsyncResponseConsumer extends AbstractAsyncResponseConsumer<Response, byte[]> {
        private final URI uri;
        private final HttpContext context;

        public ResponseAsyncResponseConsumer(@Nonnull URI uri, HttpContext context) {
            super(new BasicAsyncEntityConsumer());
            this.uri = uri;
            this.context = context;
        }

        @Override
        protected Response buildResult(HttpResponse response, byte[] entity, org.apache.hc.core5.http.ContentType contentType) {
            return new DefaultResponse(this.uri, this.context, response, entity, contentType);
        }

        @Override
        public void informationResponse(HttpResponse response, HttpContext context) throws HttpException, IOException {

        }
    }

    /**
     * 构建使用指定 SSL 上下文的可关闭异步 HTTP 客户端。
     * 使用连接池管理器并设置 TLS 策略后立即启动。
     */
    static CloseableHttpAsyncClient buildClient(SSLContext sslContext) {
        final CloseableHttpAsyncClient client = HttpAsyncClients.custom()
                .setConnectionManager(
                        PoolingAsyncClientConnectionManagerBuilder.create()
                                .setTlsStrategy(new BasicClientTlsStrategy(sslContext))
                                .build()
                )
                .build();
        client.start();
        return client;
    }

    /**
     * 支持取消底层 Apache {@link Future} 的 {@link CompletableFuture} 扩展。
     * 取消时同时取消 Apache 侧的异步操作。
     */
    private static class ResponseFuture extends CompletableFuture<Response> {
        private volatile Future<?> future;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            final Future<?> future = this.future;
            if (future != null) {
                future.cancel(mayInterruptIfRunning);
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }

    /**
     * 流式处理场景下的 {@link CompletableFuture} 扩展，同样支持取消底层 Apache Future。
     */
    private static class HandlerFuture<T> extends CompletableFuture<T> {
        private volatile Future<?> future;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            final Future<?> future = this.future;
            if (future != null) {
                future.cancel(mayInterruptIfRunning);
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }

    /**
     * 从 Apache HC 5 的 {@link HttpResponse} 构建通用的 {@link cn.maarlakes.common.http.HttpResponse}。
     */
    private static cn.maarlakes.common.http.HttpResponse createResponseInfo(HttpResponse response, HttpContext context, URI uri) {
        final HttpHeaders headers = toHttpHeaders(response);
        return new DefaultHttpResponse(
                response.getCode(),
                response.getReasonPhrase(),
                headers,
                uri,
                Cookies.parseFromHeaders(headers),
                getRemoteAddress(context)
        );
    }

    private static HttpHeaders toHttpHeaders(HttpResponse response) {
        final Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (org.apache.hc.core5.http.Header header : response.getHeaders()) {
            map.computeIfAbsent(header.getName(), k -> new ArrayList<>()).add(header.getValue());
        }
        return DefaultHttpHeaders.fromMultiMap(map);
    }

    private static SocketAddress getRemoteAddress(HttpContext context) {
        final EndpointDetails endpoint = (EndpointDetails) context.getAttribute(HttpClientContext.CONNECTION_ENDPOINT);
        if (endpoint == null) {
            return null;
        }
        return endpoint.getRemoteAddress();
    }


    /**
     * 流式响应消费者，将响应体块逐块推送到 {@link cn.maarlakes.common.http.PushBodySink}，
     * 由 {@link ResponseHandler} 实时消费。适用于大文件或流式数据处理。
     */
    private static class StreamingResponseConsumer<T> implements AsyncResponseConsumer<T> {
        private final Request request;
        private final ResponseHandler<T> handler;
        private final cn.maarlakes.common.http.PushBodySink sink = new cn.maarlakes.common.http.PushBodySink();
        private volatile CompletableFuture<T> handlerFuture;

        StreamingResponseConsumer(Request request, ResponseHandler<T> handler) {
            this.request = request;
            this.handler = handler;
        }

        @Override
        public void consumeResponse(HttpResponse response, EntityDetails entityDetails,
                                    HttpContext context, FutureCallback<T> resultCallback) {
            log.trace("StreamingResponseConsumer.consumeResponse: 状态码={}", response.getCode());
            try {
                final cn.maarlakes.common.http.HttpResponse httpResponse = createResponseInfo(response, context, request.getUri());
                handlerFuture = handler.handle(httpResponse, sink);
                handlerFuture.whenComplete((val, err) -> {
                    if (err != null) {
                        if (err instanceof Exception) {
                            resultCallback.failed((Exception) err);
                        } else {
                            resultCallback.failed(new HttpClientException(err.getMessage(), err));
                        }
                    } else {
                        resultCallback.completed(val);
                    }
                });
            } catch (Exception e) {
                resultCallback.failed(e);
            }
        }

        @Override
        public void updateCapacity(CapacityChannel capacityChannel) throws IOException {
            capacityChannel.update(Integer.MAX_VALUE);
        }

        @Override
        public void consume(ByteBuffer data) throws IOException {
            final byte[] bytes = new byte[data.remaining()];
            data.get(bytes);
            sink.pushChunk(bytes, 0, bytes.length);
        }

        @Override
        public void streamEnd(List<? extends org.apache.hc.core5.http.Header> trailers) throws HttpException, IOException {
            sink.complete();
        }

        @Override
        public void failed(Exception cause) {
            log.error("流式响应消费失败: {}", cause.getMessage(), cause);
            sink.fail(cause);
        }

        @Override
        public void releaseResources() {
        }

        @Override
        public void informationResponse(HttpResponse response, HttpContext context) throws HttpException, IOException {
        }
    }

    /**
     * 基于 Apache HC 5 响应的 {@link Response} 适配实现。
     * 响应体在构造时已完全读入字节数组。
     */
    private static class DefaultResponse implements Response {

        private final URI uri;
        private final HttpContext context;
        private final HttpResponse response;
        private final ResponseBody body;

        private DefaultResponse(@Nonnull URI uri, HttpContext context, @Nonnull HttpResponse response, byte[] bodyBuffer, org.apache.hc.core5.http.ContentType contentType) {
            this.uri = uri;
            this.context = context;
            this.response = response;
            this.body = new ByteArrayResponseBody(
                    bodyBuffer == null ? new byte[0] : bodyBuffer,
                    ContentType.parse(contentType.toString()),
                    this.getHeaders().getHeader(HttpHeaderNames.CONTENT_ENCODING)
            );
        }

        @Override
        public int getStatusCode() {
            return this.response.getCode();
        }

        @Override
        public String getStatusText() {
            return this.response.getReasonPhrase();
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
            return toHttpHeaders(this.response);
        }

        @Nonnull
        @Override
        public List<? extends cn.maarlakes.common.http.Cookie> getCookies() {
            final CookieStore cookieStore = (CookieStore) context.getAttribute(HttpClientContext.COOKIE_STORE);
            final List<Cookie> cookies = cookieStore.getCookies();
            if (CollectionUtils.isEmpty(cookies)) {
                return new ArrayList<>();
            }
            final List<cn.maarlakes.common.http.Cookie> list = new ArrayList<>();
            for (Cookie cookie : cookies) {
                final cn.maarlakes.common.http.Cookie.Builder builder = cn.maarlakes.common.http.Cookie.builder(cookie.getName())
                        .value(cookie.getValue())
                        .domain(cookie.getDomain())
                        .path(cookie.getPath())
                        .isSecure(cookie.isSecure());
                if (cookie.getCreationDate() != null && cookie.getExpiryDate() != null) {
                    builder.maxAge((cookie.getExpiryDate().getTime() - cookie.getCreationDate().getTime()) / 1000L);
                }
                builder.isHttpOnly(Boolean.parseBoolean(cookie.getAttribute("httponly")));
                String sameSite = cookie.getAttribute("samesite");
                if (sameSite == null) {
                    sameSite = cookie.getAttribute("same-site");
                }
                if (sameSite != null) {
                    builder.sameSite(cn.maarlakes.common.http.Cookie.SameSite.of(sameSite));
                }
                list.add(builder.build());
            }
            return list;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            final EndpointDetails endpoint = (EndpointDetails) this.context.getAttribute(HttpClientContext.CONNECTION_ENDPOINT);
            if (endpoint == null) {
                return null;
            }
            return endpoint.getRemoteAddress();
        }
    }
}
