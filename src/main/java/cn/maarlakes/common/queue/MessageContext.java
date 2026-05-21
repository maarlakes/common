package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

/**
 * 消息上下文，在 {@link QueueListener#onMessage} 回调中传递。
 *
 * <p>调用 {@link #acknowledge()} 确认消息已成功处理。
 * 未确认的消息会在监听器回调完成后重新入队。
 *
 * @author linjpxc
 */
public interface MessageContext<T> {

    /** 队列名称 */
    @Nonnull
    String getQueueName();

    /** 消息内容 */
    @Nonnull
    T getMessage();

    /** 确认消息已成功处理，确认后不再重新投递 */
    void acknowledge();
}
