package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * @author linjpxc
 */
public abstract class AbstractTopicQueue<T> implements TopicQueue<T> {

    protected final List<QueueListener<T>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(@Nonnull QueueListener<T> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(@Nonnull QueueListener<T> listener) {
        this.listeners.remove(listener);
    }

    protected void onMessage(@Nonnull QueueContext<T> context) {
        for (QueueListener<T> listener : this.listeners) {
            listener.onTake(context);
        }
    }
}
