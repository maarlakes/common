package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.ByteArrayOutputStream;
import java.net.SocketAddress;
import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
public class FilterableHttpClient implements HttpClient {

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

    private final HttpClient delegate;
    private final List<HttpFilter> interceptors;

    public FilterableHttpClient(@Nonnull HttpClient delegate, @Nonnull List<HttpFilter> interceptors) {
        this.delegate = delegate;
        this.interceptors = interceptors;
    }

    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        return new DefaultChain(this.delegate, this.interceptors, 0).doFilter(request, config, BUFFERING_HANDLER);
    }

    @Nonnull
    @Override
    public <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler) {
        return new DefaultChain(this.delegate, this.interceptors, 0).doFilter(request, config, handler);
    }

    @Override
    public void close() {
        this.delegate.close();
    }

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
                return this.interceptors.get(this.index).doFilter(request, config, handler,
                        new DefaultChain(this.delegate, this.interceptors, this.index + 1));
            }
            return this.delegate.execute(request, config, handler);
        }
    }

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
