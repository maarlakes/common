package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.util.EventListener;

/**
 * 消息监听器。注册到 {@link MessageQueue} 后，消息到达时自动回调。
 *
 * @author linjpxc
 */
public interface QueueListener<T> extends EventListener {

    /** 处理收到的消息。通过 {@link MessageContext#acknowledge()} 确认消息 */
    void onMessage(@Nonnull MessageContext<T> context);
}
