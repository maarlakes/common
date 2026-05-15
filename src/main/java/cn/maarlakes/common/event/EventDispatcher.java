package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * @author linjpxc
 */
public interface EventDispatcher {

    <E> void dispatch(@Nonnull EventInvoker invoker, @Nonnull E event);

    <E> CompletableFuture<Void> dispatchAsync(@Nonnull EventInvoker invoker, @Nonnull E event);
}
