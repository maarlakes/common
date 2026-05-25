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
 * <h3>线程池配置</h3>
 * <p>默认线程池参数：核心线程数 = CPU 核数，最大线程数 = CPU 核数 × 2，
 * 空闲存活时间 = 1 分钟，任务队列 = {@link SynchronousQueue}（直接提交策略）。
 *
 * <h3>队列缓存</h3>
 * <p>通过 {@link ConcurrentHashMap#computeIfAbsent} 实现线程安全的懒创建。
 * 同名队列只创建一次，后续调用返回缓存实例。
 *
 * <h3>速率限制</h3>
 * <p>可选的 {@link RateLimiterFactory} 参数，传入 {@code null} 表示不限制消费速率。
 * 速率限制器仅在 {@link DelayedQueue} 上生效。
 *
 * @see cn.maarlakes.common.queue.redis.RedissonQueueClient
 * @author linjpxc
 */
public class MemoryQueueClient implements QueueClient {

    private static final Logger log = LoggerFactory.getLogger(MemoryQueueClient.class);

    private final ExecutorFactory executorFactory;
    private final RateLimiterFactory rateLimiterFactory;
    private final Map<String, MessageQueue<?>> messageQueues = new ConcurrentHashMap<>();
    private final Map<String, DelayedQueue<?>> delayedQueues = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * 使用默认线程池（核心 = CPU 核数，最大 = CPU 核数 × 2）创建客户端。
     */
    public MemoryQueueClient() {
        this(new SharedExecutorFactory(new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2,
                1L, TimeUnit.MINUTES, new SynchronousQueue<>())));
    }

    /**
     * 使用默认线程池和自定义速率限制器工厂创建客户端。
     *
     * @param rateLimiterFactory 速率限制器工厂
     */
    public MemoryQueueClient(@Nonnull RateLimiterFactory rateLimiterFactory) {
        this(new SharedExecutorFactory(new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2,
                1L, TimeUnit.MINUTES, new SynchronousQueue<>())), rateLimiterFactory);
    }

    /**
     * 使用自定义执行器工厂创建客户端，不限制消费速率。
     *
     * @param executorFactory 执行器工厂
     */
    public MemoryQueueClient(@Nonnull ExecutorFactory executorFactory) {
        this(executorFactory, null);
    }

    /**
     * @param executorFactory    执行器工厂
     * @param rateLimiterFactory 速率限制器工厂，{@code null} 表示不限制
     */
    public MemoryQueueClient(@Nonnull ExecutorFactory executorFactory, RateLimiterFactory rateLimiterFactory) {
        this.executorFactory = executorFactory;
        this.rateLimiterFactory = rateLimiterFactory;
    }

    /**
     * 获取或创建指定名称的消息队列。
     *
     * <p>通过 {@link ConcurrentHashMap#computeIfAbsent} 保证同名队列只创建一次。
     * 客户端关闭后调用会抛出 {@link IllegalStateException}。
     */
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

    /**
     * 获取或创建指定名称的延迟队列。
     *
     * <p>通过 {@link ConcurrentHashMap#computeIfAbsent} 保证同名队列只创建一次。
     * 如果提供了 {@link RateLimiterFactory}，会为每个延迟队列创建独立的速率限制器。
     */
    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name) {
        if (this.closed.get()) {
            throw new IllegalStateException("MemoryQueueClient has been closed");
        }
        return (DelayedQueue<T>) this.delayedQueues.computeIfAbsent(name, k -> {
            log.debug("创建内存延迟队列: {}", k);
            return new MemoryDelayQueue<>(name, this.executorFactory.createExecutor(),
                    this.rateLimiterFactory == null ? null : this.rateLimiterFactory.createLimiter());
        });
    }

    /**
     * 关闭客户端。通过 {@code compareAndSet} 保证只执行一次。
     * 关闭顺序：先关闭所有队列实例，再清空缓存 Map。
     */
    @Override
    public void close() throws IOException {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        log.info("关闭 MemoryQueueClient，共 {} 个消息队列，{} 个延迟队列", this.messageQueues.size(), this.delayedQueues.size());
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
