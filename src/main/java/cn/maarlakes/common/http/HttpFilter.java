package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface HttpFilter {

    @Nonnull
    <T> CompletableFuture<T> doFilter(@Nonnull Request request, RequestConfig config,
                                       @Nonnull ResponseHandler<T> handler, @Nonnull Chain chain);

    interface Chain {
        @Nonnull
        <T> CompletableFuture<T> doFilter(@Nonnull Request request, RequestConfig config,
                                           @Nonnull ResponseHandler<T> handler);
    }
}
