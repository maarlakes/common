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
 * <p>队列名称以 {@code namespace:name} 格式作为 Redis key。
 * 每个队列使用独立的线程池执行消息分发，支持自定义 {@link Codec} 进行序列化。
 *
 * @author linjpxc
 */
public class RedissonQueueClient implements QueueClient {

    private static final Logger log = LoggerFactory.getLogger(RedissonQueueClient.class);

    private static ExecutorFactory defaultExecutorFactory() {
        return new SharedExecutorFactory(new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2, 1L, TimeUnit.MINUTES, new SynchronousQueue<>()));
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

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace) {
        this(redissonClient, new Kryo5Codec(), namespace, defaultExecutorFactory(), null);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull RateLimiterFactory rateLimiterFactory) {
        this(redissonClient, new Kryo5Codec(), namespace, defaultExecutorFactory(), rateLimiterFactory);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace) {
        this(redissonClient, codec, namespace, defaultExecutorFactory(), null);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace, @Nonnull RateLimiterFactory rateLimiterFactory) {
        this(redissonClient, codec, namespace, defaultExecutorFactory(), rateLimiterFactory);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory) {
        this(redissonClient, new Kryo5Codec(), namespace, executorFactory, null);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory, @Nonnull RateLimiterFactory rateLimiterFactory) {
        this(redissonClient, new Kryo5Codec(), namespace, executorFactory, rateLimiterFactory);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory, RateLimiterFactory rateLimiterFactory) {
        this.redissonClient = redissonClient;
        this.codec = codec;
        this.namespace = namespace;
        this.executorFactory = executorFactory;
        this.rateLimiterFactory = rateLimiterFactory;
    }

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
            return new RedissonDelayQueue<>(k, queue, this.redissonClient.getDelayedQueue(queue), executor, this.rateLimiterFactory == null ? null : this.rateLimiters.computeIfAbsent(k, key -> this.rateLimiterFactory.createLimiter()));
        });
    }

    @Override
    public void close() throws IOException {
        if (!this.closed.compareAndSet(false, true)) {
            return;
        }
        log.debug("关闭 RedissonQueueClient，共 {} 个消息队列，{} 个延迟队列", this.messageQueues.size(), this.delayedQueues.size());
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

    private String getName(@Nonnull String name) {
        if (this.namespace.endsWith(":")) {
            return this.namespace + name;
        }
        return this.namespace + ":" + name;
    }
}
