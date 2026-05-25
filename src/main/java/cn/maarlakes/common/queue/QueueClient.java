package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.io.IOException;

/**
 * 队列客户端，用于创建和缓存 {@link MessageQueue} 和 {@link DelayedQueue} 实例。
 *
 * <h3>缓存机制</h3>
 * <p>同名队列只会创建一次，后续调用 {@link #getQueue} 或 {@link #getDelayedQueue} 返回缓存的实例。
 * 内部使用 {@link java.util.concurrent.ConcurrentHashMap} 保证线程安全。
 *
 * <h3>生命周期</h3>
 * <p>使用完毕后必须调用 {@link #close()} 释放所有队列资源（消费线程、连接等）。
 * 关闭后再次调用 {@link #getQueue} 或 {@link #getDelayedQueue} 会抛出 {@link IllegalStateException}。
 *
 * @see MemoryQueueClient
 * @see cn.maarlakes.common.queue.redis.RedissonQueueClient
 * @author linjpxc
 */
public interface QueueClient extends AutoCloseable {

    /**
     * 获取或创建指定名称的消息队列。同名队列返回同一个缓存实例。
     *
     * @param name 队列名称，在同一客户端内唯一
     * @param <T>  消息类型
     * @return 消息队列实例
     * @throws IllegalStateException 客户端已关闭时抛出
     */
    @Nonnull
    <T> MessageQueue<T> getQueue(@Nonnull String name);

    /**
     * 获取或创建指定名称的延迟队列。同名队列返回同一个缓存实例。
     *
     * @param name 队列名称，在同一客户端内唯一
     * @param <T>  消息类型
     * @return 延迟队列实例
     * @throws IllegalStateException 客户端已关闭时抛出
     */
    @Nonnull
    <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name);

    /**
     * 关闭客户端，释放所有队列资源。关闭顺序：先关闭所有队列，再清空缓存。
     *
     * @throws IOException 关闭时发生 I/O 错误
     */
    @Override
    void close() throws IOException;
}
