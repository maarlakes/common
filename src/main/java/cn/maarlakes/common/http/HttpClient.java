package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.io.Closeable;
import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
public interface HttpClient extends Closeable {

    @Nonnull
    default CompletableFuture<Response> execute(@Nonnull Request request) {
        return execute(request, null);
    }

    @Nonnull
    CompletableFuture<Response> execute(@Nonnull Request request, RequestConfig config);

    @Nonnull
    static HttpClientBuilder builder() {
        return new DefaultHttpClientBuilder();
    }
}
