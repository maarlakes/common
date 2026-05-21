package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public class InterceptableHttpClient implements HttpClient {

    private final HttpClient delegate;
    private final List<HttpClientInterceptor> interceptors;

    public InterceptableHttpClient(@Nonnull HttpClient delegate, @Nonnull List<HttpClientInterceptor> interceptors) {
        this.delegate = delegate;
        this.interceptors = interceptors;
    }

    @Nonnull
    @Override
    public CompletionStage<? extends Response> execute(@Nonnull Request request, RequestConfig config) {
        return new DefaultChain(this.delegate, this.interceptors, 0).proceed(request, config);
    }

    @Override
    public void close() throws IOException {
        this.delegate.close();
    }

    private static class DefaultChain implements HttpClientInterceptor.Chain {
        private final HttpClient delegate;
        private final List<HttpClientInterceptor> interceptors;
        private final int index;

        DefaultChain(@Nonnull HttpClient delegate, @Nonnull List<HttpClientInterceptor> interceptors, int index) {
            this.delegate = delegate;
            this.interceptors = interceptors;
            this.index = index;
        }

        @Nonnull
        @Override
        public CompletionStage<? extends Response> proceed(@Nonnull Request request, RequestConfig config) {
            if (this.index < this.interceptors.size()) {
                return this.interceptors.get(this.index).intercept(request, config, new DefaultChain(this.delegate, this.interceptors, this.index + 1));
            }
            return this.delegate.execute(request, config);
        }
    }
}
