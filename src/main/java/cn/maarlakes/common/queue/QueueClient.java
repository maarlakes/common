package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.io.IOException;

/**
 * 队列客户端，用于创建和缓存 {@link MessageQueue} 和 {@link DelayedQueue} 实例。
 *
 * <p>同名队列只会创建一次，后续调用返回缓存的实例。
 *
 * @author linjpxc
 */
public interface QueueClient extends AutoCloseable {

    /** 获取或创建指定名称的消息队列 */
    @Nonnull
    <T> MessageQueue<T> getQueue(@Nonnull String name);

    /** 获取或创建指定名称的延迟队列 */
    @Nonnull
    <T> DelayedQueue<T> getDelayedQueue(@Nonnull String name);

    /** 关闭客户端，释放所有队列资源 */
    @Override
    void close() throws IOException;
}
