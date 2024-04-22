package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface QueueContext<T> {

    @Nonnull
    String getQueueName();

    @Nonnull
    T getMessage();
}
