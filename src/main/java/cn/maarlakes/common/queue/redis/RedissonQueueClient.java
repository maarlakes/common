package cn.maarlakes.common.queue.redis;

import cn.maarlakes.common.queue.DelayedQueue;
import cn.maarlakes.common.queue.MessageQueue;
import cn.maarlakes.common.queue.QueueClient;
import cn.maarlakes.common.utils.ExecutorFactory;
import cn.maarlakes.common.utils.RateLimiter;
import cn.maarlakes.common.utils.RateLimiterFactory;
import cn.maarlakes.common.utils.SharedExecutorFactory;
import jakarta.annotation.Nonnull;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.Kryo5Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 基于 Redisson 的 {@link QueueClient} 实现，使用 Redis 作为后端存储。
 *
 * <p>支持跨进程消息传递，消息存储在 Redis 中，可被不同 JVM 进程的生产者和消费者共享。
 *
 * <h3>namespace 策略</h3>
 * <p>队列名称以 {@code namespace:name} 格式作为 Redis key。
 * namespace 用于隔离不同应用的队列数据，避免 key 冲突。
 * 如果 namespace 已以冒号结尾，则不再重复添加。
 *
 * <h3>序列化</h3>
 * <p>默认使用 {@link Kryo5Codec} 进行消息序列化，可通过构造参数自定义 {@link Codec}。
 * 消息类需要支持所选 Codec 的序列化要求。
 *
 * <h3>执行器管理</h3>
 * <p>每个队列使用独立的 {@link Executor}（通过 {@link ExecutorFactory} 创建），
 * 避免不同队列的监听器回调相互影响。
 *
 * <h3>速率限制</h3>
 * <p>可选的 {@link RateLimiterFactory} 参数，传入 {@code null} 表示不限制消费速率。
 * 每个延迟队列创建独立的速率限制器实例。
 *
 * @see cn.maarlakes.common.queue.MemoryQueueClient
 * @author linjpxc
 */
public class RedissonQueueClient implements QueueClient {

    private static final Logger log = LoggerFactory.getLogger(RedissonQueueClient.class);

    /**
     * 创建默认执行器工厂。线程池参数：核心 = CPU 核数，最大 = CPU 核数 × 2，
     * 空闲存活时间 = 1 分钟，任务队列 = {@link SynchronousQueue}（直接提交策略）。
     */
    private static ExecutorFactory defaultExecutorFactory() {
        return new SharedExecutorFactory(new ThreadPoolExecutor(
                Runtime.getRuntime().availableProcessors(),
                Runtime.getRuntime().availableProcessors() * 2,
                1L, TimeUnit.MINUTES, new SynchronousQueue<>()));
    }

    private final Codec codec;
    private final RedissonClient redissonClient;
    private final String namespace;
    private final ExecutorFactory executorFactory;
    private final RateLimiterFactory rateLimiterFactory;
    private final Map<String, MessageQueue<?>> messageQueues = new ConcurrentHashMap<>();
    private final Map<String, DelayedQueue<?>> delayedQueues = new ConcurrentHashMap<>();
    private final Map<String, Executor> executors = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();
    private final AtomicBoolean closed = new AtomicBoolean();

    /**
     * 使用默认 Kryo5 编解码和默认线程池。
     *
     * @param redissonClient Redisson 客户端
     * @param namespace      命名空间，用于 Redis key 前缀隔离
     */
    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace) {
        this(redissonClient, new Kryo5Codec(), namespace, defaultExecutorFactory(), null);
    }

    /**
     * 使用默认 Kryo5 编解码、默认线程池和自定义速率限制器。
     *
     * @param redissonClient     Redisson 客户端
     * @param namespace          命名空间
     * @param rateLimiterFactory 速率限制器工厂
     */
    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull RateLimiterFactory rateLimiterFactory) {
        this(redissonClient, new Kryo5Codec(), namespace, defaultExecutorFactory(), rateLimiterFactory);
    }

    /**
     * 使用自定义编解码和默认线程池。
     *
     * @param redissonClient Redisson 客户端
     * @param codec          消息序列化编解码器
     * @param namespace      命名空间
     */
    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace) {
        this(redissonClient, codec, namespace, defaultExecutorFactory(), null);
    }

    /**
     * 使用自定义编解码、默认线程池和速率限制器。
     *
     * @param redissonClient     Redisson 客户端
     * @param codec              消息序列化编解码器
     * @param namespace          命名空间
     * @param rateLimiterFactory 速率限制器工厂
     */
    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace, @Nonnull RateLimiterFactory rateLimiterFactory) {
        this(redissonClient, codec, namespace, defaultExecutorFactory(), rateLimiterFactory);
    }

    /**
     * 使用默认 Kryo5 编解码和自定义执行器工厂。
     *
     * @param redissonClient  Redisson 客户端
     * @param namespace       命名空间
     * @param executorFactory 执行器工厂
     */
    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory) {
        this(redissonClient, new Kryo5Codec(), namespace, executorFactory, null);
    }

    /**
     * 使用默认 Kryo5 编解码、自定义执行器工厂和速率限制器。
     *
     * @param redissonClient     Redisson 客户端
     * @param namespace          命名空间
     * @param executorFactory    执行器工厂
     * @param rateLimiterFactory 速率限制器工厂
     */
    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory, @Nonnull RateLimiterFactory rateLimiterFactory) {
        this(redissonClient, new Kryo5Codec(), namespace, executorFactory, rateLimiterFactory);
    }

    /**
     * 完整参数构造函数。
     *
     * @param redissonClient     Redisson 客户端
     * @param codec              消息序列化编解码器
     * @param namespace          命名空间
     * @param executorFactory    执行器工厂
     * @param rateLimiterFactory 速率限制器工厂，{@code null} 表示不限制
     */
    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory, RateLimiterFactory rateLimiterFactory) {
        this.redissonClient = redissonClient;
        this.codec = codec;
        this.namespace = namespace;
        this.executorFactory = executorFactory;
        this.rateLimiterFactory = rateLimiterFactory;
    }

    /**
     * 获取或创建指定名称的消息队列。
     *
     * <p>通过双重 {@link ConcurrentHashMap#computeIfAbsent} 保证：
     * 同名队列共享同一个 {@link Executor} 和 {@link MessageQueue} 实例。
     * 客户端关闭后调用会抛出 {@link IllegalStateException}。
     */
    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> MessageQueue<T> getQueue(@Nonnull String name) {
        if (this.closed.get()) {
            throw new IllegalStateException("RedissonQueueClient has been closed");
        }
        return (MessageQueue<T>) this.messageQueues.computeIfAbsent(name, k -> {
            final Executor executor = this.executors.computeIfAbsent(k, key -> this.executorFactory.createExecutor());
            log.debug("创建 Redisson 消息队列: {}", this.getName(k));
            return new RedissonMessageQueue<>(k, this.redissonClient.getBlockingQueue(this.getName(k), this.codec), executor);
        });
    }

    /**
     * 获取或创建指定名称的延迟队列。
     *
     * <p>延迟队列需要同时创建 {@link org.redisson.api.RBlockingQueue} 和
     * {@link org.redisson.api.RDelayedQueue} 两个 Redis 数据结构。
     * 如果提供了 {@link RateLimiterFactory}，会为每个延迟队列创建独立的速率限制器。
     */
    @Nonnull
    @Override
    @SuppressWarnings("unchecked")
    public <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name) {
        if (this.closed.get()) {
            throw new IllegalStateException("RedissonQueueClient has been closed");
        }
        return (DelayedQueue<T>) this.delayedQueues.computeIfAbsent(name, k -> {
            final Executor executor = this.executors.computeIfAbsent(k, key -> this.executorFactory.createExecutor());
            final RBlockingQueue<T> queue = this.redissonClient.getBlockingQueue(this.getName(k), this.codec);
            log.debug("创建 Redisson 延迟队列: {}", this.getName(k));
            return new RedissonDelayQueue<>(k, queue, this.redissonClient.getDelayedQueue(queue), executor,
                    this.rateLimiterFactory == null ? null : this.rateLimiters.computeIfAbsent(k, key -> this.rateLimiterFactory.createLimiter()));
        });
    }

    /**
     * 关闭客户端。通过 {@code compareAndSet} 保证只执行一次。
     * 关闭顺序：先关闭所有队列实例，再清空所有缓存 Map。
     */
    @Override
    public void close() throws IOException {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        log.info("关闭 RedissonQueueClient，共 {} 个消息队列，{} 个延迟队列", this.messageQueues.size(), this.delayedQueues.size());
        for (MessageQueue<?> queue : this.messageQueues.values()) {
            if (queue != null) {
                queue.close();
            }
        }
        for (DelayedQueue<?> queue : this.delayedQueues.values()) {
            if (queue != null) {
                queue.close();
            }
        }
        this.messageQueues.clear();
        this.delayedQueues.clear();
        this.executors.clear();
        this.rateLimiters.clear();
    }

    /**
     * 拼接完整的 Redis key。格式为 {@code namespace:name}。
     * 如果 namespace 已以冒号结尾，则不再重复添加冒号。
     *
     * @param name 队列名称
     * @return 完整的 Redis key
     */
    private String getName(@Nonnull String name) {
        if (this.namespace.endsWith(":")) {
            return this.namespace + name;
        }
        return this.namespace + ":" + name;
    }
}
