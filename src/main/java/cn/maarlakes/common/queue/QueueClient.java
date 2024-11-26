package cn.maarlakes.common.queue;

import cn.maarlakes.common.utils.RateLimiterFactory;
import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface QueueClient {

    RateLimiterFactory getRateLimiterFactory();

    @Nonnull
    <T> TopicQueue<T> getQueue(@Nonnull String name);

    @Nonnull
    <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name);
}
