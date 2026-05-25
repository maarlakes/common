package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

/**
 * 消息上下文，在 {@link QueueListener#onMessage} 回调中传递，提供消息元数据和确认能力。
 *
 * <h3>确认语义</h3>
 * <ul>
 *   <li>调用 {@link #acknowledge()} 表示消息已成功处理，消息不会被重新投递</li>
 *   <li>多次调用 {@link #acknowledge()} 是安全的（幂等操作）</li>
 *   <li>如果从未调用 {@link #acknowledge()}，消息会在监听器回调完成后重新入队</li>
 * </ul>
 *
 * @param <T> 消息类型
 * @see DefaultMessageContext
 * @see QueueListener#onMessage
 * @author linjpxc
 */
public interface MessageContext<T> {

    /**
     * 队列名称。
     *
     * @return 消息所属队列的名称
     */
    @Nonnull
    String getQueueName();

    /**
     * 消息内容。
     *
     * @return 当前正在处理的消息
     */
    @Nonnull
    T getMessage();

    /**
     * 确认消息已成功处理。确认后消息不再重新投递。
     *
     * <p>此操作是幂等的，多次调用不会产生副作用。
     */
    void acknowledge();
}
