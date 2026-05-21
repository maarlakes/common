package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 消息队列基础实现，提供监听器管理（{@link CopyOnWriteArrayList}）和消息分发。
 *
 * <p>分发逻辑：遍历所有监听器逐一调用，任一监听器抛异常不影响后续监听器，
 * 但会导致 {@link #dispatchMessage} 返回 false。
 *
 * @author linjpxc
 */
public abstract class AbstractMessageQueue<T> implements MessageQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(AbstractMessageQueue.class);

    protected final List<QueueListener<T>> listeners = new CopyOnWriteArrayList<>();

    @Override
    public void addListener(@Nonnull QueueListener<T> listener) {
        this.listeners.add(listener);
    }

    @Override
    public void removeListener(@Nonnull QueueListener<T> listener) {
        this.listeners.remove(listener);
    }

    /**
     * 将消息分发给所有监听器。
     *
     * @return true 表示所有监听器均成功处理，false 表示至少一个监听器抛异常
     */
    protected boolean dispatchMessage(@Nonnull MessageContext<T> context) {
        boolean success = true;
        for (QueueListener<T> listener : this.listeners) {
            try {
                listener.onMessage(context);
            } catch (Exception e) {
                success = false;
                log.warn("队列 {} 监听器处理消息异常", context.getQueueName(), e);
            }
        }
        return success;
    }
}
