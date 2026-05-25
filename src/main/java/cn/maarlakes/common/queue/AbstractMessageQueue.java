package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 消息队列基础实现，提供监听器管理和消息分发功能。
 *
 * <h3>监听器管理</h3>
 * <p>使用 {@link CopyOnWriteArrayList} 存储监听器，保证线程安全。
 * 写操作（add/remove）会复制底层数组，适用于读多写少的监听器注册场景。
 * 读操作（分发时的迭代）使用快照，不需要加锁。
 *
 * <h3>消息分发</h3>
 * <p>遍历所有监听器逐一调用 {@link QueueListener#onMessage}。采用异常隔离策略：
 * 任一监听器抛异常会被捕获并记录日志，不影响后续监听器的消息接收。
 * 但异常会导致 {@link #dispatchMessage} 返回 {@code false}，影响 autoAck 判断。
 *
 * @param <T> 消息类型
 * @author linjpxc
 */
public abstract class AbstractMessageQueue<T> implements MessageQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractMessageQueue.class);

    protected final List<QueueListener<T>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(@Nonnull QueueListener<T> listener) {
        this.listeners.add(listener);
        if (log.isDebugEnabled()) {
            log.debug("队列 {} 添加监听器: {}", this.name(), listener.getClass().getName());
        }
    }

    @Override
    public void removeListener(@Nonnull QueueListener<T> listener) {
        this.listeners.remove(listener);
        if (log.isDebugEnabled()) {
            log.debug("队列 {} 移除监听器: {}", this.name(), listener.getClass().getName());
        }
    }

    /**
     * 将消息分发给所有已注册的监听器。
     *
     * <p>采用异常隔离策略：每个监听器独立 try-catch，一个监听器异常不会中断其他监听器的消息接收。
     * 异常会被记录为 WARN 级别日志。
     *
     * @param context 消息上下文
     * @return {@code true} 表示所有监听器均成功处理，{@code false} 表示至少一个监听器抛异常
     */
    protected boolean dispatchMessage(@Nonnull MessageContext<T> context) {
        if (log.isTraceEnabled()) {
            log.trace("队列 {} 分发消息到 {} 个监听器", context.getQueueName(), this.listeners.size());
        }
        boolean success = true;
        for (QueueListener<T> listener : this.listeners) {
            try {
                listener.onMessage(context);
            } catch (Exception e) {
                success = false;
                log.warn("队列 {} 监听器 {} 处理消息异常", context.getQueueName(), listener.getClass().getName(), e);
            }
        }
        return success;
    }
}
