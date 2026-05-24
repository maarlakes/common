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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
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
 * 基于 JDK 内置 {@link HttpURLConnection} 的 {@link HttpClient} 实现。
 *
 * <p>此实现不引入任何外部 HTTP 客户端依赖，通过将阻塞式 I/O 操作提交到 {@link Executor}
 * 来适配 {@link HttpClient} 的异步契约。作为框架的默认回退实现，当 classpath 上不存在
 * Apache HttpClient、OkHttp 等高级实现时自动生效。
 *
 * <p><b>线程安全性</b>：实例本身不可变（所有字段均为 final），多个线程可安全共享同一实例。
 * 每次 {@link #execute} 调用会创建独立的 {@link HttpURLConnection}，互不干扰。
 *
 * <p><b>资源管理</b>：当使用无参构造函数或仅传入 {@link RequestConfig} 时，内部会创建
 * {@link ForkJoinPool} 并在 {@link #close()} 时自动关闭。外部传入的 {@link Executor}
 * 则由调用方自行管理生命周期。
 *
 * <p><b>限制</b>：由于 {@link HttpURLConnection} 的设计限制，此实现不支持 HTTP/2、
 * 连接池复用、异步 I/O 等高级特性。生产环境建议使用基于 Apache HttpClient 或 OkHttp 的实现。
 *
 * @author linjpxc
 */
public class JdkHttpClient implements HttpClient {

    private static final Logger log = LoggerFactory.getLogger(JdkHttpClient.class);

    private final Executor executor;
    private final boolean ownsExecutor;
    private final SSLContext sslContext;
    private final RequestConfig defaultConfig;

    /**
     * 使用默认 {@link ForkJoinPool} 和无 SSL/请求配置创建实例。
     *
     * <p>创建的 {@link ForkJoinPool} 归此实例所有，{@link #close()} 时会自动关闭。
     */
    public JdkHttpClient() {
        this(new ForkJoinPool(), true, null, null);
    }

    /**
     * 使用默认 {@link ForkJoinPool} 和指定默认请求配置创建实例。
     *
     * @param defaultConfig 默认请求配置，用于未指定配置时的回退，可为 null
     */
    public JdkHttpClient(RequestConfig defaultConfig) {
        this(new ForkJoinPool(), true, null, defaultConfig);
    }

    /**
     * 使用指定的执行器和无 SSL/请求配置创建实例。
     *
     * <p>执行器由调用方管理，{@link #close()} 不会关闭它。
     *
     * @param executor 用于执行阻塞式 HTTP I/O 的线程池，不允许为 null
     */
    public JdkHttpClient(@Nonnull Executor executor) {
        this(executor, false, null, null);
    }

    /**
     * 使用指定的执行器和默认请求配置创建实例。
     *
     * @param executor      用于执行阻塞式 HTTP I/O 的线程池，不允许为 null
     * @param defaultConfig 默认请求配置，可为 null
     */
    public JdkHttpClient(@Nonnull Executor executor, RequestConfig defaultConfig) {
        this(executor, false, null, defaultConfig);
    }

    /**
     * 使用指定的执行器和 SSL 上下文创建实例。
     *
     * @param executor    用于执行阻塞式 HTTP I/O 的线程池，不允许为 null
     * @param sslContext  自定义 SSL 上下文，用于 HTTPS 连接，可为 null（使用 JDK 默认）
     */
    public JdkHttpClient(@Nonnull Executor executor, SSLContext sslContext) {
        this(executor, false, sslContext, null);
    }

    /**
     * 使用指定的执行器、SSL 上下文和默认请求配置创建实例。
     *
     * @param executor      用于执行阻塞式 HTTP I/O 的线程池，不允许为 null
     * @param sslContext    自定义 SSL 上下文，可为 null
     * @param defaultConfig 默认请求配置，可为 null
     */
    public JdkHttpClient(@Nonnull Executor executor, SSLContext sslContext, RequestConfig defaultConfig) {
        this(executor, false, sslContext, defaultConfig);
    }

    /**
     * 内部全参数构造函数。
     *
     * @param executor      执行阻塞式 HTTP I/O 的线程池
     * @param ownsExecutor  是否在 {@link #close()} 时关闭执行器
     * @param sslContext    自定义 SSL 上下文，可为 null
     * @param defaultConfig 默认请求配置，可为 null
     */
    JdkHttpClient(@Nonnull Executor executor, boolean ownsExecutor, SSLContext sslContext, RequestConfig defaultConfig) {
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
        this.sslContext = sslContext;
        this.defaultConfig = defaultConfig;
        log.debug("JdkHttpClient 已创建: ownsExecutor={}, sslContext={}, defaultConfig={}",
                ownsExecutor, sslContext != null, defaultConfig != null);
    }

    /**
     * 使用指定请求配置异步执行 HTTP 请求，将响应体完整缓冲到内存后返回。
     *
     * <p>当响应状态码为 407（Proxy Authentication Required）且请求配置中包含代理认证信息时，
     * 会自动通过 SPI 发现的 {@link ProxyAuthenticator} 进行代理认证并重试请求。
     *
     * @param request HTTP 请求，不允许为 null
     * @param config  请求级配置（超时、代理等），可为 null，为 null 时使用客户端默认配置
     * @return 异步响应，完成后包含完整的 {@link Response}
     */
    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        log.debug("发送请求: {} {}", request.getMethod(), request.getUri());
        final ResponseFuture future = new ResponseFuture();
        this.executor.execute(() -> {
            try {
                if (future.isCancelled()) {
                    log.trace("请求 {} {} 在执行前已被取消", request.getMethod(), request.getUri());
                    return;
                }
                final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
                final URL url = toUrl(request);
                HttpURLConnection connection = createConnection(url, request, effectiveConfig);
                future.setConnection(connection);
                Response response = doExecute(connection, request, url);

                // 收到 407 时，尝试通过 ProxyAuthenticator 进行代理认证后重试
                if (!future.isCancelled() && response.getStatusCode() == 407 && config != null && config.getProxy() != null && config.getProxyAuthentication() != null) {
                    log.debug("收到 407 代理认证要求: {} {}, 尝试代理认证", request.getMethod(), request.getUri());
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
                        log.warn("所有代理认证器均未成功: {} {}, 中止请求", request.getMethod(), request.getUri());
                        return;
                    }
                    response = doExecute(connection, request, url);
                }

                if (!future.isCancelled()) {
                    log.debug("请求 {} {} 完成, 状态码: {}", request.getMethod(), request.getUri(), response.getStatusCode());
                    future.complete(response);
                }
            } catch (Exception e) {
                if (!future.isCancelled()) {
                    log.error("请求 {} {} 失败: {}", request.getMethod(), request.getUri(), e.getMessage(), e);
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }
            }
        });
        return future;
    }

    /**
     * 使用指定请求配置异步执行 HTTP 请求，并通过 {@link ResponseHandler} 流式处理响应体。
     *
     * <p>与 {@link #execute(Request, RequestConfig)} 不同，此方法不会将响应体完整缓冲到内存，
     * 而是将底层连接交给 {@code handler} 自行消费。连接的生命周期由内部的 {@link JdkBodySink} 管理，
     * 在 {@link BodySink#consume} 完成后自动断开。
     *
     * <p>同样支持 407 代理认证的自动重试逻辑。
     *
     * @param request HTTP 请求，不允许为 null
     * @param config  请求级配置，可为 null
     * @param handler 响应处理器，不允许为 null
     * @param <T>     handler 的返回类型
     * @return 异步结果，完成后包含 handler 的处理结果
     */
    @Nonnull
    @Override
    public <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        log.debug("发送流式请求: {} {}", request.getMethod(), request.getUri());
        final HandlerFuture<T> future = new HandlerFuture<>();
        this.executor.execute(() -> {
            try {
                if (future.isCancelled()) {
                    log.trace("请求 {} {} 在执行前已被取消", request.getMethod(), request.getUri());
                    return;
                }
                final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
                final URL url = toUrl(request);
                HttpURLConnection connection = createConnection(url, request, effectiveConfig);
                future.setConnection(connection);
                HttpResponse httpResponse = doExecuteHandler(connection, request, url);

                // 收到 407 时，尝试通过 ProxyAuthenticator 进行代理认证后重试
                if (!future.isCancelled() && httpResponse.getStatusCode() == 407 && config != null && config.getProxy() != null && config.getProxyAuthentication() != null) {
                    log.debug("收到 407 代理认证要求: {} {} (流式), 尝试代理认证", request.getMethod(), request.getUri());
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
                        log.warn("所有代理认证器均未成功: {} {} (流式), 中止请求", request.getMethod(), request.getUri());
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
                            log.debug("流式请求 {} {} 处理完成", request.getMethod(), request.getUri());
                            future.complete(val);
                        }
                    });
                }
            } catch (Exception e) {
                if (!future.isCancelled()) {
                    log.error("流式请求 {} {} 失败: {}", request.getMethod(), request.getUri(), e.getMessage(), e);
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }
            }
        });
        return future;
    }

    /**
     * 关闭客户端，释放所持有的资源。
     *
     * <p>仅当此实例拥有执行器（通过无参或仅 {@link RequestConfig} 构造函数创建）时，
     * 才会关闭内部的 {@link ExecutorService}。外部传入的执行器由调用方自行管理。
     */
    @Override
    public void close() {
        if (this.ownsExecutor && this.executor instanceof ExecutorService) {
            log.debug("关闭自有的执行器");
            ((ExecutorService) executor).shutdown();
        }
        log.debug("JdkHttpClient 已关闭");
    }

    /**
     * 将请求的 URI 和查询参数组合为完整的 {@link URL}。
     *
     * <p>查询参数会正确拼接到 URI 上：如果原始 URI 已包含查询字符串，则以 {@code &} 追加；
     * 否则以 {@code ?} 开启新的查询字符串。
     */
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

    /**
     * 将 {@link HttpURLConnection} 的响应头 {@code Map<String, List<String>>} 转换为
     * 框架统一的 {@link HttpHeaders}，过滤掉 key 为 null 的条目。
     */
    private HttpHeaders toHeaders(final Map<String, List<String>> map) {
        return new DefaultHttpHeaders(
                map.entrySet().stream().filter(item -> item.getKey() != null).map(entry -> new DefaultHeader(entry.getKey(), entry.getValue())).collect(Collectors.toList())
                        .stream().collect(Collectors.toMap(DefaultHeader::getName, Function.identity()))
        );
    }

    /**
     * 执行实际的 HTTP 请求并完整读取响应体到内存。
     *
     * <p>根据响应状态码选择正确的输入流：2xx 使用 {@link HttpURLConnection#getInputStream()}，
     * 其他状态码使用 {@link HttpURLConnection#getErrorStream()}。请求完成后始终断开连接。
     */
    private Response doExecute(HttpURLConnection connection, Request request, URL url) throws Exception {
        try {
            sendRequestBody(connection, request);
            connection.connect();
            final int responseCode = connection.getResponseCode();
            log.debug("收到响应: {} {} (请求: {} {})", responseCode, connection.getResponseMessage(), request.getMethod(), url);

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

    /**
     * 执行实际的 HTTP 请求，返回 {@link HttpResponse} 但不消费响应体。
     *
     * <p>用于流式处理场景（{@link ResponseHandler}），响应体由后续的 {@link JdkBodySink} 延迟消费。
     * 注意此方法不会断开连接——连接的生命周期交给 {@link JdkBodySink} 管理。
     */
    private HttpResponse doExecuteHandler(HttpURLConnection connection, Request request, URL url) throws Exception {
        sendRequestBody(connection, request);
        connection.connect();
        final int responseCode = connection.getResponseCode();
        log.debug("收到响应(流式): {} {} (请求: {} {})", responseCode, connection.getResponseMessage(), request.getMethod(), url);
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

    /**
     * 将请求体写入连接的输出流。
     *
     * <p>如果请求未显式设置 body 但包含 form 参数，则自动构建
     * {@link UrlEncodedFormEntityBody}（{@code application/x-www-form-urlencoded}）。
     */
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

    /**
     * 根据请求配置创建并配置 {@link HttpURLConnection}。
     *
     * <p>配置包括：代理、代理认证、重定向策略、超时时间、请求头、Cookie 和 SSL。
     * 优先使用 {@code connectTimeout}，回退到 {@code requestTimeout} 作为连接超时。
     */
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
                // 未单独设置 connectTimeout 时，用 requestTimeout 作为连接超时的回退
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

    /**
     * 通过 SPI 发现的 {@link ProxyAuthenticator} 为连接设置代理认证头。
     *
     * <p>遍历所有已注册的 {@link ProxyAuthenticator}，使用第一个 {@code supported()} 返回 true 的
     * 认证器进行认证。
     */
    private void setProxyAuthorization(@Nonnull HttpURLConnection connection, @Nonnull Proxy proxy, @Nonnull ProxyAuthentication authentication) {
        for (ProxyAuthenticator authenticator : SpiServiceLoader.loadShared(ProxyAuthenticator.class, this.getClass().getClassLoader())) {
            if (authenticator.supported(proxy, authentication)) {
                authenticator.authenticate(connection, proxy, authentication);
                break;
            }
        }
    }

    /**
     * 可取消的 {@link CompletableFuture}，持有 {@link HttpURLConnection} 引用以便在取消时断开连接。
     *
     * <p>用于 {@link #execute(Request, RequestConfig)} 方法，返回完整的 {@link Response}。
     */
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

    /**
     * 可取消的 {@link CompletableFuture}，用于 {@link ResponseHandler} 场景。
     *
     * <p>与 {@link ResponseFuture} 类似，但泛型参数为 {@code T} 以适配 handler 的返回类型。
     */
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

    /**
     * 基于 {@link HttpURLConnection} 的 {@link BodySink} 实现，将连接的输入流以分块方式
     * 交给 {@link BodyConsumer} 处理。
     *
     * <p>在 {@link #consume} 完成后（无论成功或失败）始终断开连接。
     */
    private static class JdkBodySink implements BodySink {
        private static final Logger log = LoggerFactory.getLogger(JdkBodySink.class);
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
                log.error("消费响应体时出错: {}", e.getMessage(), e);
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

    /**
     * 临时 {@link Response} 适配器，将 {@link HttpResponse} 包装为 {@link Response} 以供
     * {@link ProxyAuthenticator#authenticate} 使用。
     *
     * <p>代理认证只需要响应的状态码、状态文本和头信息，不需要真实的响应体，因此返回空 body。
     */
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

    /**
     * 基于 {@link HttpURLConnection} 响应的完整 {@link Response} 实现。
     *
     * <p>响应体在构造时即完整读入内存（{@code byte[]}），适合非流式场景。
     * 远程地址通过 {@link Lazy} 延迟解析，避免在不需要时执行 DNS 查询。
     */
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
