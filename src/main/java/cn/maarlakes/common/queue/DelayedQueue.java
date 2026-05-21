package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * 延迟队列接口。消息在指定的延迟时间后才会被消费。
 *
 * <p>如果消息实现了 {@link java.util.concurrent.Delayed} 接口，
 * 调用 {@link #offer(Object)} 时会自动使用消息自带的延迟时间。
 *
 * @author linjpxc
 */
public interface DelayedQueue<T> extends MessageQueue<T> {

    /** 按指定延迟时间投递消息 */
    boolean offer(@Nonnull T value, @Nonnull Duration delay);

    /** 异步按指定延迟时间投递消息 */
    CompletionStage<Boolean> offerAsync(@Nonnull T value, @Nonnull Duration delay);
}
