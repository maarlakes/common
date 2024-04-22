package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public class DefaultQueueContext<T> implements QueueContext<T> {

    private final String queueName;
    private final T message;

    public DefaultQueueContext(@Nonnull String queueName, @Nonnull T message) {
        this.queueName = queueName;
        this.message = message;
    }

    @Nonnull
    @Override
    public String getQueueName() {
        return this.queueName;
    }

    @Nonnull
    @Override
    public T getMessage() {
        return this.message;
    }
}
