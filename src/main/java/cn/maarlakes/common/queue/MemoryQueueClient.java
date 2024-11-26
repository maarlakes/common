package cn.maarlakes.common.queue;

import cn.maarlakes.common.utils.ExecutorFactory;
import cn.maarlakes.common.utils.RateLimiterFactory;
import cn.maarlakes.common.utils.SharedExecutorFactory;
import jakarta.annotation.Nonnull;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author linjpxc
 */
public class MemoryQueueClient implements QueueClient {

    private final ExecutorFactory executorFactory;
    private final RateLimiterFactory rateLimiterFactory;
    private final Map<String, TopicQueue<?>> topicQueue = new ConcurrentHashMap<>();
    private final Map<String, DelayedQueue<?>> delayedQueue = new ConcurrentHashMap<>();

    public MemoryQueueClient() {
        this(new SharedExecutorFactory(new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2, 1L, TimeUnit.MINUTES, new SynchronousQueue<>())));
    }

    public MemoryQueueClient(@Nonnull RateLimiterFactory rateLimiterFactory) {
        this(new SharedExecutorFactory(new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2, 1L, TimeUnit.MINUTES, new SynchronousQueue<>())), rateLimiterFactory);
    }

    public MemoryQueueClient(@Nonnull ExecutorFactory executorFactory) {
        this(executorFactory, null);
    }

    public MemoryQueueClient(@Nonnull ExecutorFactory executorFactory, RateLimiterFactory rateLimiterFactory) {
        this.executorFactory = executorFactory;
        this.rateLimiterFactory = rateLimiterFactory;
    }

    @Override
    public RateLimiterFactory getRateLimiterFactory() {
        return this.rateLimiterFactory;
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> TopicQueue<T> getQueue(@Nonnull String name) {
        return (TopicQueue<T>) this.topicQueue.computeIfAbsent(name, k -> new MemoryTopicQueue<>(name, this.executorFactory.createExecutor()));
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name) {
        return (DelayedQueue<T>) this.delayedQueue.computeIfAbsent(name, k -> new MemoryDelayQueue<>(name, this.executorFactory.createExecutor(), this.rateLimiterFactory == null ? null : this.rateLimiterFactory.createLimiter()));
    }
}
