package cn.maarlakes.common.http;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
public interface BodySink {

    <T> CompletableFuture<T> consume(@Nonnull BodyConsumer<T> consumer);
}
