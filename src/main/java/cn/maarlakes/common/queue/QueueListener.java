package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.util.EventListener;

/**
 * 消息监听器。注册到 {@link MessageQueue} 后，消息到达时自动回调。
 *
 * <h3>注册与回调</h3>
 * <p>通过 {@link MessageQueue#addListener} 注册到队列。首个监听器注册时消费线程自动启动，
 * 后续消息通过消费线程获取并分发给所有已注册的监听器。
 *
 * <h3>异常隔离</h3>
 * <p>任一监听器在 {@link #onMessage} 中抛出异常不会影响其他监听器的消息接收，
 * 异常会被捕获并记录日志。但异常会导致该消息的自动确认失败（如果开启了 autoAck），
 * 消息可能被重新入队。
 *
 * @param <T> 消息类型
 * @see AbstractMessageQueue
 * @see MessageContext
 * @author linjpxc
 */
public interface QueueListener<T> extends EventListener {

    /**
     * 处理收到的消息。在 Executor 线程中被调用，不阻塞消费线程。
     *
     * <p>处理完成后需调用 {@link MessageContext#acknowledge()} 确认消息，
     * 否则消息会被重新入队（除非开启了 autoAck 且所有监听器均无异常）。
     *
     * @param context 消息上下文，包含队列名称、消息内容和确认能力
     */
    void onMessage(@Nonnull MessageContext<T> context);
}
