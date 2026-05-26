package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * 延迟队列接口。消息在指定的延迟时间后才会被消费者获取。
 *
 * <h3>延迟机制</h3>
 * <p>消息投递后不会立即可见，需要等待指定的延迟时间到期后才能被消费。
 * 延迟时间的指定有两种方式：
 * <ul>
 *   <li>通过 {@link #offer(Object, Duration)} 显式指定延迟时间</li>
 *   <li>如果消息实现了 {@link java.util.concurrent.Delayed} 接口，
 *       调用 {@link #offer(Object)} 时会自动使用消息自带的延迟时间</li>
 * </ul>
 *
 * @param <T> 消息类型
 * @see MemoryDelayQueue
 * @author linjpxc
 */
public interface DelayedQueue<T> extends MessageQueue<T> {

    /**
     * 按指定延迟时间投递消息。消息在延迟时间到期后才可被消费。
     *
     * @param value 要投递的消息，不允许为 null
     * @param delay 延迟时间，不允许为 null。{@link Duration#ZERO} 表示立即投递
     * @return 投递成功返回 {@code true}
     */
    boolean offer(@Nonnull T value, @Nonnull Duration delay);

    /**
     * 异步按指定延迟时间投递消息。
     *
     * @param value 要投递的消息，不允许为 null
     * @param delay 延迟时间，不允许为 null
     * @return 异步投递结果，{@code true} 表示成功
     */
    CompletionStage<Boolean> offerAsync(@Nonnull T value, @Nonnull Duration delay);
}
