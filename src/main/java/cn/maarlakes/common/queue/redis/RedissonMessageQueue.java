package cn.maarlakes.common.queue.redis;

import cn.maarlakes.common.queue.AbstractBlockingQueue;
import jakarta.annotation.Nonnull;
import org.redisson.api.RBlockingQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/**
 * 基于 Redisson {@link RBlockingQueue} 的消息队列实现，支持跨进程消息传递。
 *
 * <p>消息存储在 Redis 中，可被不同 JVM 进程的生产者和消费者共享。
 * Redisson 内部处理分布式锁和连接管理。
 *
 * <p>Redis key 格式由 {@link RedissonQueueClient} 的 namespace 决定（如 {@code "namespace:queueName"}）。
 * 序列化方式由 Redisson 的 {@link org.redisson.client.codec.Codec} 控制，默认使用 {@link org.redisson.codec.Kryo5Codec}。
 *
 * @param <T> 消息类型
 * @see cn.maarlakes.common.queue.MemoryMessageQueue
 * @author linjpxc
 */
class RedissonMessageQueue<T> extends AbstractBlockingQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(RedissonMessageQueue.class);

    private final String name;
    private final RBlockingQueue<T> queue;

    /**
     * @param name     队列名称
     * @param queue    Redisson 阻塞队列实例
     * @param executor 用于异步执行监听器回调的执行器
     */
    public RedissonMessageQueue(@Nonnull String name, @Nonnull RBlockingQueue<T> queue, @Nonnull Executor executor) {
        super(executor, null);
        this.queue = queue;
        this.name = name;
    }

    /**
     * 阻塞获取一条消息，对应 Redis 的 BLPOP 命令。
     * 由消费线程循环调用，队列无消息时阻塞等待。
     */
    @Nonnull
    @Override
    protected T take() throws Exception {
        return this.queue.take();
    }

    /**
     * 将未确认的消息重新投递到 Redis 队列。
     */
    @Override
    protected void reOffer(@Nonnull T value) {
        if (log.isDebugEnabled()) {
            log.debug("Redisson 队列 {} 重新投递消息: {}", this.name, value);
        }
        this.queue.offer(value);
    }

    @Nonnull
    @Override
    public String name() {
        return this.name;
    }

    @Override
    public int size() {
        return this.queue.size();
    }

    @Override
    public CompletionStage<Integer> sizeAsync() {
        return this.queue.sizeAsync().toCompletableFuture();
    }

    @Override
    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    @Override
    public CompletionStage<Boolean> isEmptyAsync() {
        // Redisson RBlockingQueue 的 isEmpty() 已足够快，直接包装为已完成异步结果
        return CompletableFuture.completedFuture(this.queue.isEmpty());
    }

    @Override
    public boolean offer(@Nonnull T value) {
        return this.queue.offer(value);
    }

    /**
     * 异步投递消息，委托给 Redisson 的异步 API。
     */
    @Override
    public CompletionStage<Boolean> offerAsync(@Nonnull T value) {
        return this.queue.offerAsync(value).toCompletableFuture();
    }

    @Override
    public T poll() {
        return this.queue.poll();
    }

    /**
     * 异步拉取消息，委托给 Redisson 的异步 API。
     */
    @Override
    public CompletionStage<T> pollAsync() {
        return this.queue.pollAsync().toCompletableFuture();
    }

    @Override
    public void clear() {
        if (log.isDebugEnabled()) {
            log.debug("Redisson 队列 {} 清空", this.name);
        }
        this.queue.clear();
    }

    @Override
    public CompletionStage<Void> clearAsync() {
        return CompletableFuture.runAsync(this.queue::clear, this.executor);
    }

    @Override
    public boolean remove(@Nonnull T value) {
        return this.queue.remove(value);
    }

    @Override
    public CompletionStage<Boolean> removeAsync(@Nonnull T value) {
        return this.queue.removeAsync(value).toCompletableFuture();
    }

    @Override
    public boolean removeAll(@Nonnull Collection<? extends T> values) {
        return this.queue.removeAll(values);
    }

    @Override
    public CompletionStage<Boolean> removeAllAsync(@Nonnull Collection<? extends T> values) {
        return this.queue.removeAllAsync(values).toCompletableFuture();
    }

    @Override
    public List<? extends T> removeIf(@Nonnull Predicate<T> predicate) {
        final List<T> list = new ArrayList<>();
        // 同时执行判断和收集：满足条件的消息从队列移除并加入结果列表
        this.queue.removeIf(item -> {
            if (predicate.test(item)) {
                list.add(item);
                return true;
            }
            return false;
        });
        return list;
    }

    @Override
    public boolean contains(@Nonnull T value) {
        return this.queue.contains(value);
    }

    @Override
    public CompletionStage<Boolean> containsAsync(@Nonnull T value) {
        return this.queue.containsAsync(value).toCompletableFuture();
    }
}
