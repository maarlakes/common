package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
@FunctionalInterface
public interface ResponseHandler<T> {

    CompletableFuture<T> handle(@Nonnull HttpResponse response, @Nonnull BodySink body);
}
