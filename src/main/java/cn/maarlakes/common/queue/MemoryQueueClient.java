package cn.maarlakes.common.queue;

import cn.maarlakes.common.function.Function1;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * @author linjpxc
 */
public class MemoryQueueClient implements QueueClient {

    private final Function1<String, Executor> executorFactory;
    private final Map<String, TopicQueue<?>> topicQueue = new ConcurrentHashMap<>();
    private final Map<String, DelayedQueue<?>> delayedQueue = new ConcurrentHashMap<>();

    public MemoryQueueClient() {
        this(value -> new ForkJoinPool());
    }

    public MemoryQueueClient(@Nonnull Function1<String, Executor> executorFactory) {
        this.executorFactory = executorFactory;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> TopicQueue<T> getQueue(@Nonnull String name) {
        return (TopicQueue<T>) this.topicQueue.computeIfAbsent(name, k -> new MemoryTopicQueue<>(name, executorFactory.apply(name)));
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name) {
        return (DelayedQueue<T>) this.delayedQueue.computeIfAbsent(name, k -> new MemoryDelayQueue<>(name, executorFactory.apply(name)));
    }
}
