package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
public interface HttpClient extends AutoCloseable {

    @Nonnull
    default CompletableFuture<Response> execute(@Nonnull Request request) {
        return execute(request, null);
    }

    @Nonnull
    CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config);

    @Nonnull
    <T> CompletableFuture<T> execute(@Nonnull Request request, RequestConfig config, @Nonnull ResponseHandler<T> handler);

    @Override
    void close() throws RuntimeException;

    @Nonnull
    static HttpClientBuilder builder() {
        return new DefaultHttpClientBuilder();
    }
}
