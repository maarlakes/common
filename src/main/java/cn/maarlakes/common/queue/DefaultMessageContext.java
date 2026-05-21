package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 默认消息上下文实现，跟踪消息确认状态。
 *
 * <p>监听器处理完成后调用 {@link #complete}，根据确认状态决定是否重新入队：
 * <ul>
 *   <li>已调用 {@link #acknowledge()} → 不重新入队</li>
 *   <li>autoAck 且所有监听器无异常 → 不重新入队</li>
 *   <li>其他情况 → 触发 {@code onNotAcknowledged} 回调（通常为 reOffer）</li>
 * </ul>
 *
 * @author linjpxc
 */
public class DefaultMessageContext<T> implements MessageContext<T> {

    private static final Logger log = LoggerFactory.getLogger(DefaultMessageContext.class);

    private final String queueName;
    private final T message;
    private final AtomicBoolean acknowledged = new AtomicBoolean();
    private final Runnable onNotAcknowledged;

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
     * 监听器处理完成后的回调，决定是否重新入队。
     *
     * @param success  所有监听器是否均成功处理
     * @param autoAck  是否开启自动确认
     */
    void complete(boolean success, boolean autoAck) {
        if (this.acknowledged.get()) {
            return;
        }
        if (autoAck && success) {
            return;
        }
        log.warn("队列 {} 消息未确认，重新入队: {}", this.queueName, this.message);
        this.onNotAcknowledged.run();
    }
}
