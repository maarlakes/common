package cn.maarlakes.common.http.apache;

import cn.maarlakes.common.factory.datetime.DateTimeFactories;
import cn.maarlakes.common.http.*;
import cn.maarlakes.common.http.body.multipart.FilePart;
import cn.maarlakes.common.http.body.multipart.MultipartBody;
import cn.maarlakes.common.http.body.multipart.MultipartPart;
import cn.maarlakes.common.spi.SpiServiceLoader;
import cn.maarlakes.common.utils.CollectionUtils;
import jakarta.annotation.Nonnull;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpInetConnection;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CookieStore;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.client.utils.URIUtils;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.FormBodyPartBuilder;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * 基于 Apache HttpClient 4.x（同步阻塞 API）的 {@link HttpClient} 适配实现。
 *
 * <p>将统一的 {@link HttpClient} 异步契约适配到 Apache HC 4 的同步 {@link org.apache.http.client.HttpClient}。
 * 由于底层是同步阻塞的，所有请求通过外部提供的 {@link Executor} 调度，以避免阻塞调用线程。
 *
 * <p>构造时可传入自定义 {@link Executor}（如线程池），若未指定则默认使用 {@link ForkJoinPool}。
 * 当本实例拥有 executor（{@code ownsExecutor = true}）时，{@link #close()} 会同时关闭底层客户端和 executor。
 *
 * <p>线程安全性：底层 Apache {@link org.apache.http.client.HttpClient} 自身是线程安全的，
 * 本实例可在多线程间共享。每个请求在独立的 executor 调度单元中执行。
 *
 * @author linjpxc
 */
public class ApacheHttpClient4 implements HttpClient {

    private static final Logger log = LoggerFactory.getLogger(ApacheHttpClient4.class);

    private final org.apache.http.client.HttpClient client;
    private final Executor executor;
    private final boolean ownsExecutor;
    private final RequestConfig defaultConfig;

    /**
     * 使用默认 HttpClient 和 ForkJoinPool 构建，本实例拥有 executor 生命周期。
     */
    public ApacheHttpClient4() {
        this(HttpClientBuilder.create().build(), new ForkJoinPool(), true, null);
    }

    /**
     * 使用指定 Executor，本实例不拥有 executor 生命周期。
     *
     * @param executor 用于调度同步请求的执行器，不允许为 null
     */
    public ApacheHttpClient4(@Nonnull Executor executor) {
        this(HttpClientBuilder.create().build(), executor, false, null);
    }

    /**
     * 使用指定 Executor 和默认请求配置。
     *
     * @param executor      用于调度同步请求的执行器，不允许为 null
     * @param defaultConfig 默认请求配置，可为 null
     */
    public ApacheHttpClient4(@Nonnull Executor executor, RequestConfig defaultConfig) {
        this(HttpClientBuilder.create().build(), executor, false, defaultConfig);
    }

    /**
     * 使用指定 Executor 和 SSL 上下文，本实例拥有 executor 和 HttpClient 生命周期。
     *
     * @param executor   用于调度同步请求的执行器，不允许为 null
     * @param sslContext SSL/TLS 上下文，不允许为 null
     */
    public ApacheHttpClient4(@Nonnull Executor executor, SSLContext sslContext) {
        this(buildClient(sslContext), executor, true, null);
    }

    /**
     * 使用外部提供的 HttpClient 和 ForkJoinPool，本实例拥有 executor 生命周期。
     *
     * @param client 已配置的 Apache HttpClient 4.x 实例，不允许为 null
     */
    public ApacheHttpClient4(@Nonnull org.apache.http.client.HttpClient client) {
        this(client, new ForkJoinPool(), true, null);
    }

    /**
     * 使用外部提供的 HttpClient 和默认配置。
     *
     * @param client        已配置的 Apache HttpClient 4.x 实例，不允许为 null
     * @param defaultConfig 默认请求配置，可为 null
     */
    public ApacheHttpClient4(@Nonnull org.apache.http.client.HttpClient client, RequestConfig defaultConfig) {
        this(client, new ForkJoinPool(), true, defaultConfig);
    }

    /**
     * 使用外部提供的 HttpClient 和 Executor，本实例不拥有任何生命周期。
     *
     * @param client   已配置的 Apache HttpClient 4.x 实例，不允许为 null
     * @param executor 用于调度同步请求的执行器，不允许为 null
     */
    public ApacheHttpClient4(@Nonnull org.apache.http.client.HttpClient client, @Nonnull Executor executor) {
        this(client, executor, false, null);
    }

    /**
     * 使用外部提供的 HttpClient、Executor 和默认配置，不拥有任何生命周期。
     *
     * @param client        已配置的 Apache HttpClient 4.x 实例，不允许为 null
     * @param executor      用于调度同步请求的执行器，不允许为 null
     * @param defaultConfig 默认请求配置，可为 null
     */
    public ApacheHttpClient4(@Nonnull org.apache.http.client.HttpClient client, @Nonnull Executor executor, RequestConfig defaultConfig) {
        this(client, executor, false, defaultConfig);
    }

    /**
     * 全参数构造方法，定义组件所有权。
     *
     * @param client        已配置的 Apache HttpClient 4.x 实例，不允许为 null
     * @param executor      用于调度同步请求的执行器，不允许为 null
     * @param ownsExecutor  是否在 close 时关闭 executor
     * @param defaultConfig 默认请求配置，可为 null
     */
    ApacheHttpClient4(@Nonnull org.apache.http.client.HttpClient client, @Nonnull Executor executor, boolean ownsExecutor, RequestConfig defaultConfig) {
        this.client = client;
        this.executor = executor;
        this.ownsExecutor = ownsExecutor;
        this.defaultConfig = defaultConfig;
    }

    /**
     * 构建使用指定 SSL 上下文的 Apache HttpClient 4.x 实例。
     */
    private static org.apache.http.client.HttpClient buildClient(SSLContext sslContext) {
        final HttpClientBuilder builder = HttpClientBuilder.create();
        if (sslContext != null) {
            builder.setSSLSocketFactory(new SSLConnectionSocketFactory(sslContext));
        }
        return builder.build();
    }

    /**
     * 发送请求并完整缓冲响应体到内存。
     *
     * <p>实现策略：在 {@link Executor} 中同步执行 Apache HC 4 请求，通过
     * {@link org.apache.http.client.ResponseHandler} 将响应体读入字节数组，
     * 最终通过 {@link CompletableFuture} 异步返回。
     *
     * @param request HTTP 请求，不允许为 null
     * @param config  请求级配置，可为 null（使用客户端默认配置）
     * @return 异步响应，完成后包含完整响应
     */
    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        final ResponseFuture future = new ResponseFuture();
        log.debug("提交请求到执行器: {} {}", request.getMethod(), request.getUri());
        this.executor.execute(() -> {
            final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
            InputStream contentStream = null;
            try {
                final PreparedRequest prepared = prepareRequest(request, effectiveConfig);
                contentStream = prepared.contentStream;
                final HttpUriRequest uriRequest = prepared.request;
                future.request = uriRequest;
                if (future.isCancelled()) {
                    return;
                }

                log.debug("执行请求: {} {}", request.getMethod(), request.getUri());
                final Response result = this.client.execute(determineTarget(uriRequest), uriRequest, (ResponseHandler<Response>) response -> new DefaultResponse(request.getUri(), response, prepared.context), prepared.context);
                log.debug("收到响应: {} {} -> {}", request.getMethod(), request.getUri(), result.getStatusCode());
                if (!future.isCancelled()) {
                    future.complete(result);
                }
            } catch (Exception e) {
                log.error("请求执行失败: {} {} - {}", request.getMethod(), request.getUri(), e.getMessage(), e);
                if (!future.isCancelled()) {
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }
            } finally {
                if (contentStream != null) {
                    try {
                        contentStream.close();
                    } catch (IOException ignored) {
                        // 关闭请求体输入流失败不影响结果
                    }
                }
            }
        });
        return future;
    }

    /**
     * 发送请求并通过 {@link cn.maarlakes.common.http.ResponseHandler} 流式处理响应体。
     *
     * <p>实现策略：在 {@link Executor} 中同步执行请求，将响应体的 {@link java.io.InputStream}
     * 包装为 {@link InputStreamBodySink}，由 handler 实时消费。适合处理大文件或流式数据。
     *
     * @param request HTTP 请求，不允许为 null
     * @param config  请求级配置，可为 null
     * @param handler 响应流处理器，不允许为 null
     * @param <T>     handler 的返回类型
     * @return 异步结果，完成后包含 handler 的处理结果
     */
    @Nonnull
    @Override
    public <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull cn.maarlakes.common.http.ResponseHandler<T> handler) {
        final HandlerFuture<T> future = new HandlerFuture<>();
        log.debug("提交流式请求到执行器: {} {}", request.getMethod(), request.getUri());
        this.executor.execute(() -> {
            final RequestConfig effectiveConfig = RequestConfigs.merge(this.defaultConfig, config);
            InputStream contentStream = null;
            try {
                final PreparedRequest prepared = prepareRequest(request, effectiveConfig);
                contentStream = prepared.contentStream;
                final HttpUriRequest uriRequest = prepared.request;
                future.request = uriRequest;
                if (future.isCancelled()) {
                    return;
                }

                log.debug("执行流式请求: {} {}", request.getMethod(), request.getUri());
                this.client.execute(determineTarget(uriRequest), uriRequest, response -> {
                    try {
                        final cn.maarlakes.common.http.HttpResponse httpResponse = createResponseInfo(request.getUri(), response, prepared.context);
                        final BodySink body = new InputStreamBodySink(response.getEntity() != null ? response.getEntity().getContent() : null);
                        final CompletableFuture<T> result = handler.handle(httpResponse, body);
                        result.whenComplete((val, err) -> {
                            if (err != null) {
                                log.error("流式处理器失败: {} {} - {}", request.getMethod(), request.getUri(), err.getMessage(), err);
                                future.completeExceptionally(err);
                            } else {
                                log.debug("流式请求完成: {} {}", request.getMethod(), request.getUri());
                                future.complete(val);
                            }
                        });
                    } catch (Exception e) {
                        log.error("流式响应处理失败: {} {} - {}", request.getMethod(), request.getUri(), e.getMessage(), e);
                        future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                    }
                    return null;
                }, prepared.context);
            } catch (Exception e) {
                log.error("流式请求执行失败: {} {} - {}", request.getMethod(), request.getUri(), e.getMessage(), e);
                if (!future.isCancelled()) {
                    future.completeExceptionally(new HttpClientException(e.getMessage(), e));
                }
            } finally {
                if (contentStream != null) {
                    try {
                        contentStream.close();
                    } catch (IOException ignored) {
                        // 关闭请求体输入流失败不影响结果
                    }
                }
            }
        });
        return future;
    }

    /**
     * 关闭底层 Apache HttpClient 和（若拥有生命周期的）Executor。
     *
     * <p>关闭顺序：先关闭 HTTP 客户端释放连接，再关闭 Executor 停止接受新任务。
     * 关闭后不应再发起请求。
     */
    @Override
    public void close() {
        if (this.client instanceof AutoCloseable) {
            try {
                log.debug("关闭 Apache HttpClient 4.x");
                ((AutoCloseable) this.client).close();
            } catch (Exception e) {
                log.error("关闭 Apache HttpClient 4.x 失败: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        }
        if (this.ownsExecutor && this.executor instanceof ExecutorService) {
            log.debug("关闭自有的 ExecutorService");
            ((ExecutorService) this.executor).shutdown();
        }
    }


    /**
     * 将通用 {@link ContentType} 转换为 Apache HC 4 的 {@link org.apache.http.entity.ContentType}。
     *
     * @param contentType 通用内容类型，不允许为 null
     * @return Apache HC 4 对应的内容类型
     */
    public static org.apache.http.entity.ContentType toApacheContentType(@Nonnull ContentType contentType) {
        final org.apache.http.entity.ContentType result = org.apache.http.entity.ContentType.create(contentType.getMediaType(), contentType.getCharset());
        if (CollectionUtils.isNotEmpty(contentType.getParameters())) {
            return result.withParameters(
                    contentType.getParameters().stream().map(item -> new BasicNameValuePair(item.getName(), item.getValue()))
                            .toArray(org.apache.http.NameValuePair[]::new)
            );
        }
        return result;
    }

    /**
     * 将通用 {@link RequestConfig} 转换为 Apache HC 4 的 {@link org.apache.http.client.config.RequestConfig}。
     * 仅转换非 null 的配置项，null 值使用 Apache 默认值。
     */
    @SuppressWarnings("DuplicatedCode")
    private static org.apache.http.client.config.RequestConfig to(RequestConfig config) {
        if (config == null) {
            return null;
        }
        final org.apache.http.client.config.RequestConfig.Builder builder = org.apache.http.client.config.RequestConfig.custom();
        if (config.isRedirectsEnabled() != null) {
            builder.setRedirectsEnabled(config.isRedirectsEnabled());
        }
        if (config.getRequestTimeout() != null) {
            builder.setConnectionRequestTimeout((int) config.getRequestTimeout().toMillis());
        }
        if (config.getResponseTimeout() != null) {
            builder.setSocketTimeout((int) config.getResponseTimeout().toMillis());
        }
        if (config.getConnectTimeout() != null) {
            builder.setConnectTimeout((int) config.getConnectTimeout().toMillis());
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
     * 将请求 URI 和查询参数合并构建完整 URI。
     */
    @Nonnull
    private static URI toUri(@Nonnull Request request) throws URISyntaxException {
        final URIBuilder builder = new URIBuilder(request.getUri());
        if (CollectionUtils.isNotEmpty(request.getQueryParams())) {
            for (NameValuePair param : request.getQueryParams()) {
                builder.addParameter(param.getName(), param.getValue());
            }
        }
        return builder.build();
    }

    /**
     * 封装 Apache HC 4 的请求对象、HTTP 上下文和请求体输入流。
     * contentStream 需在请求完成后手动关闭。
     */
    @SuppressWarnings("DuplicatedCode")
    private static final class PreparedRequest {
        final HttpUriRequest request;
        final HttpClientContext context;
        final InputStream contentStream;

        PreparedRequest(HttpUriRequest request, HttpClientContext context, InputStream contentStream) {
            this.request = request;
            this.context = context;
            this.contentStream = contentStream;
        }
    }

    /**
     * 将通用 {@link Request} 转换为 Apache HC 4 的 {@link HttpUriRequest}。
     *
     * <p>处理顺序：URI -> 请求头 -> 字符集 -> 表单参数 -> 请求体 -> Cookie -> 代理认证。
     * 每次调用创建独立的 {@link HttpClientContext} 和 {@link BasicCookieStore}，
     * 确保请求间 cookie 隔离。
     *
     * <p>对于非 multipart 的请求体，返回的 contentStream 需由调用方在请求完成后关闭。
     */
    private PreparedRequest prepareRequest(@Nonnull Request request, RequestConfig effectiveConfig) throws Exception {
        log.trace("准备请求: {} {}", request.getMethod(), request.getUri());
        final RequestBuilder builder = RequestBuilder.create(request.getMethod().name())
                .setUri(toUri(request));
        settingHeader(builder, request);
        if (request.getCharset() != null) {
            builder.setCharset(request.getCharset());
        }
        settingFormParams(builder, request);
        InputStream contentStream = null;
        if (request.getBody() != null) {
            if (request.getBody() instanceof MultipartBody) {
                settingMultipart(builder, (MultipartBody) request.getBody(), request.getCharset());
            } else {
                contentStream = request.getBody().getContentStream();
                if (contentStream != null) {
                    final ContentType ct = request.getBody().getContentType();
                    builder.setEntity(new InputStreamEntity(contentStream, ct != null ? toApacheContentType(ct) : null));
                }
            }
        }
        final HttpClientContext context = HttpClientContext.create();
        // 每次请求使用独立的 CookieStore，避免并发场景下的 cookie 串扰
        context.setCookieStore(new BasicCookieStore());
        final org.apache.http.client.config.RequestConfig requestConfig = to(effectiveConfig);
        if (requestConfig != null) {
            context.setRequestConfig(requestConfig);
        }
        if (effectiveConfig != null && effectiveConfig.getProxy() != null && effectiveConfig.getProxyAuthentication() != null) {
            for (Apache4ProxyAuthenticator authenticator : SpiServiceLoader.loadShared(Apache4ProxyAuthenticator.class, this.getClass().getClassLoader())) {
                if (authenticator.supported(effectiveConfig.getProxy(), effectiveConfig.getProxyAuthentication())) {
                    authenticator.authenticate(context, effectiveConfig.getProxy(), effectiveConfig.getProxyAuthentication());
                    break;
                }
            }
        }

        if (CollectionUtils.isNotEmpty(request.getCookies())) {
            builder.addHeader("Cookie", request.getCookies().stream().map(item -> item.name() + "=" + item.value()).collect(Collectors.joining(";")));
        }
        return new PreparedRequest(builder.build(), context, contentStream);
    }

    /**
     * 构建 multipart/form-data 请求体，处理 FilePart 和普通 Part 两种类型。
     * FilePart 使用 {@link FileBody}，其他类型使用 {@link InputStreamBody}。
     */
    private static void settingMultipart(@Nonnull RequestBuilder builder, @Nonnull MultipartBody body, Charset charset) {
        if (CollectionUtils.isNotEmpty(body.getContent())) {
            final MultipartEntityBuilder multipartEntityBuilder = MultipartEntityBuilder.create();
            multipartEntityBuilder.setContentType(org.apache.http.entity.ContentType.parse(ContentTypes.toString(body.getContentType())));
            for (MultipartPart<?> part : body.getContent()) {
                ContentBody contentBody;
                if (part instanceof FilePart) {
                    if (part.getContentType() == null) {
                        contentBody = new FileBody(((FilePart) part).getContent());
                    } else {
                        contentBody = new FileBody(((FilePart) part).getContent(), toApacheContentType(part.getContentType()), part.getFilename());
                    }
                } else {
                    if (part.getContentType() == null) {
                        contentBody = new InputStreamBody(part.getContentStream(), part.getFilename());
                    } else {
                        contentBody = new InputStreamBody(part.getContentStream(), toApacheContentType(part.getContentType()), part.getFilename());
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
            builder.setEntity(multipartEntityBuilder.build());
        }
    }

    /**
     * 设置表单参数到请求构建器。
     */
    private static void settingFormParams(@Nonnull RequestBuilder builder, @Nonnull Request request) {
        if (CollectionUtils.isNotEmpty(request.getFormParams())) {
            for (NameValuePair param : request.getFormParams()) {
                builder.addParameter(param.getName(), param.getValue());
            }
        }
    }

    /**
     * 设置请求头到请求构建器，支持同名头多值。
     */
    private static void settingHeader(@Nonnull RequestBuilder builder, @Nonnull Request request) {
        if (!request.getHeaders().isEmpty()) {
            for (Header header : request.getHeaders()) {
                for (String value : header.getValues()) {
                    builder.addHeader(header.getName(), value);
                }
            }
        }
    }

    /**
     * 从请求 URI 中提取目标主机，用于虚拟主机和代理场景。
     * 若 URI 不是绝对路径则返回 null，由 HttpClient 自行确定目标。
     */
    private static HttpHost determineTarget(final HttpUriRequest request) throws ClientProtocolException {
        HttpHost target = null;

        final URI uri = request.getURI();
        if (uri.isAbsolute()) {
            target = URIUtils.extractHost(uri);
            if (target == null) {
                throw new ClientProtocolException("URI does not specify a valid host name: "
                        + uri);
            }
        }
        return target;
    }

    private static HttpHeaders toHttpHeaders(HttpResponse response) {
        final Map<String, List<String>> map = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (org.apache.http.Header header : response.getAllHeaders()) {
            map.computeIfAbsent(header.getName(), k -> new ArrayList<>()).add(header.getValue());
        }
        return DefaultHttpHeaders.fromMultiMap(map);
    }

    /**
     * 支持取消底层 Apache {@link HttpUriRequest} 的 {@link CompletableFuture} 扩展。
     * 取消时通过 {@link HttpUriRequest#abort()} 中断进行中的请求。
     */
    private static final class ResponseFuture extends CompletableFuture<Response> {
        private HttpUriRequest request;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (mayInterruptIfRunning) {
                final HttpUriRequest request = this.request;
                if (request != null) {
                    request.abort();
                }
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }

    /**
     * 流式处理场景下的 {@link CompletableFuture} 扩展，同样支持取消底层请求。
     */
    private static final class HandlerFuture<T> extends CompletableFuture<T> {
        private HttpUriRequest request;

        @Override
        public boolean cancel(boolean mayInterruptIfRunning) {
            if (mayInterruptIfRunning) {
                final HttpUriRequest request = this.request;
                if (request != null) {
                    request.abort();
                }
            }
            return super.cancel(mayInterruptIfRunning);
        }
    }

    /**
     * 从 Apache HC 4 的 {@link HttpResponse} 构建通用的 {@link cn.maarlakes.common.http.HttpResponse}。
     * 从上下文中提取远程地址和 Cookie 信息。
     */
    private static cn.maarlakes.common.http.HttpResponse createResponseInfo(URI uri, HttpResponse response, HttpContext context) {
        final Object attribute = context.getAttribute("http.connection");
        final SocketAddress remoteAddress;
        if (attribute instanceof HttpInetConnection) {
            final HttpInetConnection connection = (HttpInetConnection) attribute;
            remoteAddress = new InetSocketAddress(connection.getRemoteAddress(), connection.getRemotePort());
        } else {
            remoteAddress = null;
        }
        final HttpHeaders httpHeaders = toHttpHeaders(response);

        final CookieStore cookieStore = (CookieStore) context.getAttribute(HttpClientContext.COOKIE_STORE);
        final List<Cookie> cookies = new ArrayList<>();
        if (cookieStore != null && CollectionUtils.isNotEmpty(cookieStore.getCookies())) {
            for (org.apache.http.cookie.Cookie cookie : cookieStore.getCookies()) {
                final cn.maarlakes.common.http.Cookie.Builder builder = cn.maarlakes.common.http.Cookie.builder(cookie.getName())
                        .value(cookie.getValue())
                        .domain(cookie.getDomain())
                        .path(cookie.getPath())
                        .isSecure(cookie.isSecure());
                if (cookie.getExpiryDate() != null) {
                    builder.expires(DateTimeFactories.fromEpochMilli(cookie.getExpiryDate().getTime()));
                }
                cookies.add(builder.build());
            }
        }

        return new DefaultHttpResponse(
                response.getStatusLine().getStatusCode(),
                response.getStatusLine().getReasonPhrase(),
                httpHeaders,
                uri,
                cookies,
                remoteAddress
        );
    }

    /**
     * 基于 Apache HC 4 响应的 {@link Response} 适配实现。
     * 响应体在构造时通过 {@link EntityUtils#toByteArray} 完全读入内存。
     */
    private static class DefaultResponse implements Response {
        private final URI uri;
        private final HttpResponse response;
        private final HttpContext context;
        private final SocketAddress remoteAddress;
        private final ResponseBody body;

        private DefaultResponse(URI uri, HttpResponse response, HttpContext context) {
            this.uri = uri;
            this.response = response;
            final Object attribute = context.getAttribute("http.connection");
            if (attribute instanceof HttpInetConnection) {
                final HttpInetConnection connection = (HttpInetConnection) attribute;
                remoteAddress = new InetSocketAddress(connection.getRemoteAddress(), connection.getRemotePort());
            } else {
                remoteAddress = null;
            }
            final HttpEntity entity = response.getEntity();
            byte[] buffer;
            if (entity == null) {
                buffer = new byte[0];
            } else {
                try {
                    buffer = EntityUtils.toByteArray(entity);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
            this.body = new ByteArrayResponseBody(
                    buffer,
                    Optional.ofNullable(response.getEntity()).map(HttpEntity::getContentType).map(Object::toString).map(ContentType::parse).orElse(null),
                    this.getHeaders().getHeader(HttpHeaderNames.CONTENT_ENCODING)
            );
            this.context = context;
        }

        @Override
        public int getStatusCode() {
            return this.response.getStatusLine().getStatusCode();
        }

        @Override
        public String getStatusText() {
            return this.response.getStatusLine().getReasonPhrase();
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
        public List<? extends Cookie> getCookies() {
            final CookieStore cookieStore = (CookieStore) context.getAttribute(HttpClientContext.COOKIE_STORE);
            final List<org.apache.http.cookie.Cookie> cookies = cookieStore.getCookies();
            if (CollectionUtils.isEmpty(cookies)) {
                return new ArrayList<>();
            }
            final List<Cookie> list = new ArrayList<>();
            for (org.apache.http.cookie.Cookie cookie : cookies) {
                final cn.maarlakes.common.http.Cookie.Builder builder = cn.maarlakes.common.http.Cookie.builder(cookie.getName())
                        .value(cookie.getValue())
                        .domain(cookie.getDomain())
                        .path(cookie.getPath())
                        .isSecure(cookie.isSecure());
                if (cookie.getExpiryDate() != null) {
                    builder.expires(DateTimeFactories.fromEpochMilli(cookie.getExpiryDate().getTime()));
                }
                list.add(builder.build());
            }
            return list;
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return this.remoteAddress;
        }
    }
}
