package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * 基于 {@link HttpClient} 的过滤器装饰器，通过责任链模式实现请求/响应拦截。
 *
 * <p>设计思路：将过滤器逻辑与具体 HTTP 客户端实现完全解耦。
 * 所有 HTTP 客户端实现只负责协议通信，不感知过滤器的存在；
 * {@code FilterableHttpClient} 作为装饰器，在底层客户端外层包装过滤器链。
 *
 * <p>过滤器链的执行流程：
 * <pre>
 *   Request → Filter1 → Filter2 → ... → FilterN → Delegate Client → Response
 * </pre>
 * 每个过滤器可以在请求发送前和响应返回后执行自定义逻辑（如日志、认证、重试等）。
 *
 * <p>对于不需要自定义 {@link ResponseHandler} 的调用（即返回完整 {@link Response} 的重载），
 * 使用内置的 {@link #BUFFERING_HANDLER} 将响应体完整读入内存后构建 {@link Response}。
 * 这是因为过滤器链需要一个可重复读取的响应体，而底层流只能消费一次。
 *
 * @author linjpxc
 */
public class FilterableHttpClient implements HttpClient {

    private static final Logger log = LoggerFactory.getLogger(FilterableHttpClient.class);

    /**
     * 内置的缓冲处理器：将响应体完整读入字节数组，构建可重复读取的 {@link Response}。
     *
     * <p>用于不需要调用方提供 {@link ResponseHandler} 的 execute 重载。
     * 通过 {@link BodyConsumer} 回调逐块接收数据，在 {@link BodyConsumer#onComplete()} 时
     * 一次性构建包含完整响应体的 {@link ByteArrayResponseBody}。
     *
     * <p>同时通过 SPI 加载的 {@link cn.maarlakes.common.http.encoder.ResponseBodyEncoder}
     * 处理 Content-Encoding（gzip/deflate/brotli）解码。
     */
    private static final ResponseHandler<Response> BUFFERING_HANDLER = (httpResponse, body) -> body.consume(new BodyConsumer<Response>() {
        private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        @Override
        public void onChunk(@Nonnull byte[] data, int offset, int length) {
            buffer.write(data, offset, length);
        }

        @Override
        public Response onComplete() {
            final byte[] bytes = buffer.toByteArray();
            final ResponseBody responseBody = new ByteArrayResponseBody(
                    bytes,
                    Optional.ofNullable(httpResponse.getHeaders().getHeader(HttpHeaderNames.CONTENT_TYPE))
                            .map(Header::get).map(ContentType::parse).orElse(null),
                    httpResponse.getHeaders().getHeader(HttpHeaderNames.CONTENT_ENCODING)
            );
            return new HttpResponseAdapter(httpResponse, responseBody);
        }
    });

    /** 被装饰的底层 HTTP 客户端，实际执行网络请求。 */
    private final HttpClient delegate;

    /** 按顺序执行的过滤器列表。 */
    private final List<HttpFilter> interceptors;

    public FilterableHttpClient(@Nonnull HttpClient delegate, @Nonnull List<HttpFilter> interceptors) {
        this.delegate = delegate;
        this.interceptors = interceptors;
        log.debug("FilterableHttpClient 已创建, 包含 {} 个过滤器: {}",
                interceptors.size(), interceptors);
    }

    /**
     * 执行请求并返回完整的缓冲响应。使用 {@link #BUFFERING_HANDLER} 自动缓冲响应体。
     */
    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        log.trace("通过过滤器链执行 {} {} ({} 个过滤器)",
                request.getMethod(), request.getUri(), interceptors.size());
        return new DefaultChain(this.delegate, this.interceptors, 0).doFilter(request, config, BUFFERING_HANDLER);
    }

    /**
     * 执行请求并通过自定义 handler 处理流式响应体。
     *
     * <p>与 {@link #execute(Request, RequestConfig)} 不同，此方法不缓冲完整响应体，
     * handler 直接消费底层流，适合处理大文件等场景。
     */
    @Nonnull
    @Override
    public <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        log.trace("通过过滤器链执行 {} {} ({} 个过滤器, 自定义 handler)",
                request.getMethod(), request.getUri(), interceptors.size());
        return new DefaultChain(this.delegate, this.interceptors, 0).doFilter(request, config, handler);
    }

    /**
     * 关闭底层客户端。过滤器本身不持有需要释放的资源。
     */
    @Override
    public void close() {
        log.debug("关闭 FilterableHttpClient, 委托给底层客户端");
        this.delegate.close();
    }

    /**
     * 责任链的链式传递实现。
     *
     * <p>每个 {@link DefaultChain} 实例代表链中的一个位置。
     * 调用 {@link #doFilter} 时：
     * <ul>
     *   <li>如果当前位置有过滤器，调用该过滤器并传入下一个链节点</li>
     *   <li>如果已到达链尾，直接委托给底层客户端执行</li>
     * </ul>
     */
    private static class DefaultChain implements HttpFilter.Chain {
        private final HttpClient delegate;
        private final List<HttpFilter> interceptors;
        private final int index;

        DefaultChain(@Nonnull HttpClient delegate, @Nonnull List<HttpFilter> interceptors, int index) {
            this.delegate = delegate;
            this.interceptors = interceptors;
            this.index = index;
        }

        @Nonnull
        @Override
        public <T> CompletableFuture<T> doFilter(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
            if (this.index < this.interceptors.size()) {
                log.trace("链位置 {}: 调用过滤器 {}",
                        this.index, this.interceptors.get(this.index).getClass().getSimpleName());
                return this.interceptors.get(this.index).doFilter(request, config, handler,
                        new DefaultChain(this.delegate, this.interceptors, this.index + 1));
            }
            log.trace("链末尾: 委托给底层客户端执行 {} {}", request.getMethod(), request.getUri());
            return this.delegate.execute(request, config, handler);
        }
    }

    /**
     * 将 {@link HttpResponse}（流式响应）适配为 {@link Response}（完整响应）。
     *
     * <p>在过滤器链的缓冲模式下中，底层客户端返回的是流式的 {@link HttpResponse}，
     * 需要适配为包含完整响应体的 {@link Response} 接口。
     * 此适配器将状态码、头部、Cookie 等委托给原始 {@link HttpResponse}，
     * 仅替换 body 为已缓冲的 {@link ResponseBody}。
     */
    private static class HttpResponseAdapter implements Response {
        private final HttpResponse httpResponse;
        private final ResponseBody body;

        HttpResponseAdapter(@Nonnull HttpResponse httpResponse, @Nonnull ResponseBody body) {
            this.httpResponse = httpResponse;
            this.body = body;
        }

        @Nonnull
        @Override
        public ResponseBody getBody() {
            return this.body;
        }

        @Override
        public int getStatusCode() {
            return this.httpResponse.getStatusCode();
        }

        @Override
        public String getStatusText() {
            return this.httpResponse.getStatusText();
        }

        @Override
        public URI getUri() {
            return this.httpResponse.getUri();
        }

        @Nonnull
        @Override
        public HttpHeaders getHeaders() {
            return this.httpResponse.getHeaders();
        }

        @Nonnull
        @Override
        public List<? extends Cookie> getCookies() {
            return this.httpResponse.getCookies();
        }

        @Override
        public SocketAddress getRemoteAddress() {
            return this.httpResponse.getRemoteAddress();
        }
    }
}
