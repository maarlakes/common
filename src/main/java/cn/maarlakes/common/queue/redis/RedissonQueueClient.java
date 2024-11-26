package cn.maarlakes.common.queue.redis;

import cn.maarlakes.common.queue.DelayedQueue;
import cn.maarlakes.common.queue.QueueClient;
import cn.maarlakes.common.queue.TopicQueue;
import cn.maarlakes.common.utils.ExecutorFactory;
import cn.maarlakes.common.utils.RateLimiter;
import cn.maarlakes.common.utils.RateLimiterFactory;
import cn.maarlakes.common.utils.SharedExecutorFactory;
import jakarta.annotation.Nonnull;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.Kryo5Codec;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @author linjpxc
 */
public class RedissonQueueClient implements QueueClient {

    private final Codec codec;
    private final RedissonClient redissonClient;
    private final String namespace;
    private final ExecutorFactory executorFactory;
    private final RateLimiterFactory rateLimiterFactory;
    private final Map<String, Executor> executors = new ConcurrentHashMap<>();
    private final Map<String, RateLimiter> rateLimiters = new ConcurrentHashMap<>();

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace) {
        this(redissonClient, new Kryo5Codec(), namespace, null);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, RateLimiterFactory rateLimiterFactory) {
        this(redissonClient, new Kryo5Codec(), namespace, rateLimiterFactory);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace) {
        this(
                redissonClient, codec, namespace,
                new SharedExecutorFactory(new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2, 1L, TimeUnit.MINUTES, new SynchronousQueue<>())),
                null
        );
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace, RateLimiterFactory rateLimiterFactory) {
        this(
                redissonClient, codec, namespace,
                new SharedExecutorFactory(new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2, 1L, TimeUnit.MINUTES, new SynchronousQueue<>())),
                rateLimiterFactory
        );
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory) {
        this(redissonClient, new Kryo5Codec(), namespace, executorFactory, null);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory, RateLimiterFactory rateLimiterFactory) {
        this(redissonClient, new Kryo5Codec(), namespace, executorFactory, rateLimiterFactory);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory, RateLimiterFactory rateLimiterFactory) {
        this.redissonClient = redissonClient;
        this.codec = codec;
        this.namespace = namespace;
        this.executorFactory = executorFactory;
        this.rateLimiterFactory = rateLimiterFactory;
    }

    @Override
    public RateLimiterFactory getRateLimiterFactory() {
        return this.rateLimiterFactory;
    }

    @Nonnull
    @Override
    public <T> TopicQueue<T> getQueue(@Nonnull String name) {
        final Executor executor = this.executors.computeIfAbsent(name, k -> this.executorFactory.createExecutor());
        return new RedissonTopicQueue<>(name, this.redissonClient.getBlockingQueue(this.getName(name), this.codec), executor);
    }

    @Nonnull
    @Override
    public <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name) {
        final Executor executor = this.executors.computeIfAbsent(name, k -> this.executorFactory.createExecutor());
        final RBlockingQueue<T> queue = this.redissonClient.getBlockingQueue(this.getName(name), this.codec);
        return new RedissonDelayQueue<>(name, queue, this.redissonClient.getDelayedQueue(queue), executor, this.rateLimiterFactory == null ? null : this.rateLimiters.computeIfAbsent(name, k -> this.rateLimiterFactory.createLimiter()));
    }

    private String getName(@Nonnull String name) {
        if (this.namespace.endsWith(":")) {
            return this.namespace + name;
        }
        return this.namespace + ":" + name;
    }
}
