package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface HttpFilter {

    @Nonnull
    CompletableFuture<Response> doFilter(@Nonnull Request request, RequestConfig config, @Nonnull Chain chain);

    interface Chain {
        @Nonnull
        CompletableFuture<Response> doFilter(@Nonnull Request request, RequestConfig config);
    }
}
