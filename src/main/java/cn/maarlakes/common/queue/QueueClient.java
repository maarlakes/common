package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface QueueClient {

    @Nonnull
    <T> TopicQueue<T> getQueue(@Nonnull String name);

    @Nonnull
    <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name);
}
