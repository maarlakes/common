package cn.maarlakes.common.queue.redis;

import cn.maarlakes.common.function.Function1;
import cn.maarlakes.common.queue.DelayedQueue;
import cn.maarlakes.common.queue.QueueClient;
import cn.maarlakes.common.queue.TopicQueue;
import jakarta.annotation.Nonnull;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RedissonClient;
import org.redisson.client.codec.Codec;
import org.redisson.codec.Kryo5Codec;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * @author linjpxc
 */
public class RedissonQueueClient implements QueueClient {

    private final Codec codec = new Kryo5Codec();
    private final RedissonClient redissonClient;
    private final String namespace;
    private final Function1<String, Executor> executorFactory;
    private final Map<String, Executor> executors = new ConcurrentHashMap<>();

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace) {
        this(redissonClient, namespace, k -> new ForkJoinPool());
    }

    public RedissonQueueClient(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull Function1<String, Executor> executorFactory) {
        this.redissonClient = redissonClient;
        this.namespace = namespace;
        this.executorFactory = executorFactory;
    }

    @Nonnull
    @Override
    public <T> TopicQueue<T> getQueue(@Nonnull String name) {
        final Executor executor = this.executors.computeIfAbsent(name, this.executorFactory);
        return new RedissonTopicQueue<>(name, this.redissonClient.getBlockingQueue(this.getName(name), this.codec), executor);
    }

    @Nonnull
    @Override
    public <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name) {
        final Executor executor = this.executors.computeIfAbsent(name, this.executorFactory);
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
