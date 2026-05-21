package cn.maarlakes.common.queue;

import cn.maarlakes.common.utils.ExecutorFactory;
import cn.maarlakes.common.utils.RateLimiterFactory;
import cn.maarlakes.common.utils.SharedExecutorFactory;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 JVM 内存的 {@link QueueClient} 实现，使用 {@link ConcurrentHashMap} 缓存队列实例。
 *
 * <p>适用于单机场景，不支持跨进程消息传递。
 *
 * @author linjpxc
 */
public class MemoryQueueClient implements QueueClient {

    private static final Logger log = LoggerFactory.getLogger(MemoryQueueClient.class);

    private final ExecutorFactory executorFactory;
    private final RateLimiterFactory rateLimiterFactory;
    private final Map<String, MessageQueue<?>> messageQueues = new ConcurrentHashMap<>();
    private final Map<String, DelayedQueue<?>> delayedQueues = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

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

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> MessageQueue<T> getQueue(@Nonnull String name) {
        if (this.closed.get()) {
            throw new IllegalStateException("MemoryQueueClient has been closed");
        }
        return (MessageQueue<T>) this.messageQueues.computeIfAbsent(name, k -> {
            log.debug("创建内存消息队列: {}", k);
            return new MemoryMessageQueue<>(name, this.executorFactory.createExecutor());
        });
    }

    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name) {
        if (this.closed.get()) {
            throw new IllegalStateException("MemoryQueueClient has been closed");
        }
        return (DelayedQueue<T>) this.delayedQueues.computeIfAbsent(name, k -> {
            log.debug("创建内存延迟队列: {}", k);
            return new MemoryDelayQueue<>(name, this.executorFactory.createExecutor(), this.rateLimiterFactory == null ? null : this.rateLimiterFactory.createLimiter());
        });
    }

    @Override
    public void close() throws IOException {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        log.debug("关闭 MemoryQueueClient，共 {} 个消息队列，{} 个延迟队列", this.messageQueues.size(), this.delayedQueues.size());
        for (MessageQueue<?> queue : this.messageQueues.values()) {
            queue.close();
        }
        for (DelayedQueue<?> queue : this.delayedQueues.values()) {
            queue.close();
        }
        this.messageQueues.clear();
        this.delayedQueues.clear();
    }
}
