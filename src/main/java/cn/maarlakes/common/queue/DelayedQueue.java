package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public interface DelayedQueue<T> extends TopicQueue<T> {

    boolean offer(@Nonnull T value, @Nonnull Duration delay);

    CompletionStage<Boolean> offerAsync(@Nonnull T value, @Nonnull Duration delay);
}
