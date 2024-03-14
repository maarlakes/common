package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
public interface EventPublisher {

    <E> void publish(@Nonnull E event);

    @Nonnull
    default <E> CompletionStage<Void> publishAsync(@Nonnull E event) {
        return CompletableFuture.runAsync(() -> this.publish(event));
    }

    @Nonnull
    default <E> CompletionStage<Void> publishAsync(@Nonnull E event, @Nonnull Executor executor) {
        return CompletableFuture.runAsync(() -> this.publish(event), executor);
    }
}
