package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
public class InterceptableHttpClient implements HttpClient {

    private final HttpClient delegate;
    private final List<HttpFilter> interceptors;

    public InterceptableHttpClient(@Nonnull HttpClient delegate, @Nonnull List<HttpFilter> interceptors) {
        this.delegate = delegate;
        this.interceptors = interceptors;
    }

    @Nonnull
    @Override
    public CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config) {
        return new DefaultChain(this.delegate, this.interceptors, 0).doFilter(request, config);
    }

    @Override
    public void close() throws IOException {
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
        public CompletableFuture<Response> doFilter(@Nonnull Request request, RequestConfig config) {
            if (this.index < this.interceptors.size()) {
                return this.interceptors.get(this.index).doFilter(request, config, new DefaultChain(this.delegate, this.interceptors, this.index + 1));
            }
            return this.delegate.execute(request, config);
        }
    }
}
