package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认消息上下文实现，跟踪消息确认状态并在未确认时触发重新入队。
 *
 * <p>使用 {@link AtomicBoolean} 保证 {@link #acknowledge()} 的线程安全性和幂等性：
 * 多次调用 {@code acknowledge()} 只有第一次会生效。
 *
 * @param <T> 消息类型
 * @see MessageContext
 * @author linjpxc
 */
public class DefaultMessageContext<T> implements MessageContext<T> {

    private static final Logger log = LoggerFactory.getLogger(DefaultMessageContext.class);

    private final String queueName;
    private final T message;
    private final AtomicBoolean acknowledged = new AtomicBoolean();
    private final Runnable onNotAcknowledged;

    /**
     * @param queueName          队列名称
     * @param message            消息内容
     * @param onNotAcknowledged  消息未被确认时的回调（通常是 {@code reOffer} 重新入队操作）
     */
    public DefaultMessageContext(@Nonnull String queueName, @Nonnull T message, @Nonnull Runnable onNotAcknowledged) {
        this.queueName = queueName;
        this.message = message;
        this.onNotAcknowledged = onNotAcknowledged;
    }

    @Nonnull
    @Override
    public String getQueueName() {
        return this.queueName;
    }

    @Nonnull
    @Override
    public T getMessage() {
        return this.message;
    }

    @Override
    public void acknowledge() {
        if (log.isDebugEnabled()) {
            log.debug("队列 {} 消息已确认: {}", this.queueName, this.message);
        }
        this.acknowledged.set(true);
    }

    /**
     * 监听器处理完成后的回调，根据确认状态决定是否重新入队。
     *
     * <p>决策逻辑（按优先级）：
     * <ol>
     *   <li>已调用 {@link #acknowledge()} → 不重新入队，直接返回</li>
     *   <li>autoAck 且所有监听器无异常 → 不重新入队，直接返回</li>
     *   <li>其他情况 → 触发 {@code onNotAcknowledged} 回调（通常为 reOffer 重新入队）</li>
     * </ol>
     *
     * @param success  所有监听器是否均成功处理（无异常）
     * @param autoAck  是否开启自动确认
     */
    void complete(boolean success, boolean autoAck) {
        // 已显式确认，无需其他处理
        if (this.acknowledged.get()) {
            if (log.isTraceEnabled()) {
                log.trace("队列 {} 消息已确认，跳过 complete 处理: {}", this.queueName, this.message);
            }
            return;
        }
        // 自动确认 + 所有监听器成功，视为已确认
        if (autoAck && success) {
            return;
        }
        // 未确认：触发重新入队
        log.warn("队列 {} 消息未确认（autoAck={}, success={}），重新入队: {}", this.queueName, autoAck, success, this.message);
        this.onNotAcknowledged.run();
    }
}
