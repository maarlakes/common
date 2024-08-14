package cn.maarlakes.common.queue.redis;

import cn.maarlakes.common.queue.DelayedQueue;
import cn.maarlakes.common.queue.QueueClient;
import cn.maarlakes.common.queue.TopicQueue;
import cn.maarlakes.common.utils.ExecutorFactory;
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
    private final Map<String, Executor> executors = new ConcurrentHashMap<>();

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace) {
        this(redissonClient, new Kryo5Codec(), namespace);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace) {
        this(redissonClient, codec, namespace, new SharedExecutorFactory(new ThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), Runtime.getRuntime().availableProcessors() * 2, 1L, TimeUnit.MINUTES, new SynchronousQueue<>())));
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory) {
        this(redissonClient, new Kryo5Codec(), namespace, executorFactory);
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull Codec codec, @Nonnull String namespace, @Nonnull ExecutorFactory executorFactory) {
        this.redissonClient = redissonClient;
        this.codec = codec;
        this.namespace = namespace;
        this.executorFactory = executorFactory;
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
        return new RedissonDelayQueue<>(name, queue, this.redissonClient.getDelayedQueue(queue), executor);
    }

    private String getName(@Nonnull String name) {
        if (this.namespace.endsWith(":")) {
            return this.namespace + name;
        }
        return this.namespace + ":" + name;
    }
}
