package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.Predicate;

/**
 * 消息队列接口，支持推模式（监听器）和拉模式（poll）两种消费方式。
 *
 * <h3>消费模型</h3>
 * <ul>
 *   <li><b>推模式</b>：通过 {@link #addListener} 注册 {@link QueueListener}，队列内部启动守护线程自动获取消息并分发。
 *       首个监听器注册时消费线程启动，消息通过 {@link Executor} 异步分发给所有监听器。</li>
 *   <li><b>拉模式</b>：通过 {@link #poll} / {@link #pollAsync} 主动拉取消息，调用方自行控制消费节奏。</li>
 * </ul>
 *
 * <h3>消息确认机制</h3>
 * <p>监听器收到消息后，可通过 {@link MessageContext#acknowledge()} 显式确认消息。
 * 未确认的消息会在监听器处理完成后重新入队。开启 {@link #setAutoAck(boolean) autoAck} 后，
 * 所有监听器无异常时自动确认。
 *
 * <h3>线程模型</h3>
 * <p>推模式下，消费线程为守护线程，通过 {@code take()} 阻塞获取消息后提交到 {@link Executor} 异步执行。
 * 监听器的 {@code onMessage} 回调在 Executor 线程中执行，不阻塞消费线程。
 *
 * @param <T> 消息类型
 * @see DelayedQueue
 * @see QueueClient
 * @author linjpxc
 */
public interface MessageQueue<T> extends AutoCloseable {

    /**
     * 队列名称，在同一 {@link QueueClient} 内唯一标识一个队列实例。
     *
     * @return 队列名称
     */
    @Nonnull
    String name();

    /**
     * 队列中的消息数量。
     *
     * @return 消息数量
     */
    int size();

    /**
     * 异步获取队列大小。
     *
     * @return 异步消息数量
     */
    CompletionStage<Integer> sizeAsync();

    /**
     * 队列是否为空。
     *
     * @return 队列为空时返回 {@code true}
     */
    boolean isEmpty();

    /**
     * 异步判断队列是否为空。
     *
     * @return 异步判空结果
     */
    CompletionStage<Boolean> isEmptyAsync();

    /**
     * 投递消息到队列。非阻塞操作，如果队列容量已满（有界队列场景），返回 {@code false}。
     *
     * @param value 要投递的消息，不允许为 null
     * @return 投递成功返回 {@code true}，队列已满返回 {@code false}
     */
    boolean offer(@Nonnull T value);

    /**
     * 异步投递消息。
     *
     * @param value 要投递的消息，不允许为 null
     * @return 异步投递结果，{@code true} 表示成功
     */
    CompletionStage<Boolean> offerAsync(@Nonnull T value);

    /**
     * 拉取并移除队首消息，队列为空时返回 {@code null}。非阻塞操作。
     *
     * @return 队首消息，队列为空时返回 {@code null}
     */
    T poll();

    /**
     * 异步拉取队首消息。
     *
     * @return 异步拉取结果，队列为空时完成为 {@code null}
     */
    CompletionStage<T> pollAsync();

    /** 清空队列中所有消息。 */
    void clear();

    /** 异步清空队列。 */
    CompletionStage<Void> clearAsync();

    /**
     * 从队列中移除指定消息。
     *
     * @param value 要移除的消息
     * @return 消息存在且成功移除时返回 {@code true}
     */
    boolean remove(@Nonnull T value);

    /**
     * 异步移除指定消息。
     *
     * @param value 要移除的消息
     * @return 异步移除结果
     */
    CompletionStage<Boolean> removeAsync(@Nonnull T value);

    /**
     * 从队列中批量移除消息。
     *
     * @param values 要移除的消息集合
     * @return 至少移除一个消息时返回 {@code true}
     */
    boolean removeAll(@Nonnull Collection<? extends T> values);

    /**
     * 异步批量移除消息。
     *
     * @param values 要移除的消息集合
     * @return 异步移除结果
     */
    CompletionStage<Boolean> removeAllAsync(@Nonnull Collection<? extends T> values);

    /**
     * 按条件移除消息，返回被移除的消息列表。
     *
     * @param predicate 判断条件，返回 {@code true} 的消息将被移除
     * @return 被移除的消息列表
     */
    List<? extends T> removeIf(@Nonnull Predicate<T> predicate);

    /**
     * 队列是否包含指定消息。
     *
     * @param value 要检查的消息
     * @return 包含时返回 {@code true}
     */
    boolean contains(@Nonnull T value);

    /**
     * 异步判断是否包含指定消息。
     *
     * @param value 要检查的消息
     * @return 异步包含判断结果
     */
    CompletionStage<Boolean> containsAsync(@Nonnull T value);

    /**
     * 添加消息监听器。首个监听器添加后，消费线程自动启动。
     *
     * <p>同一个监听器可以被多次添加，每次添加都会收到消息通知。
     *
     * @param listener 消息监听器
     * @throws IllegalStateException 队列已关闭时抛出
     */
    void addListener(@Nonnull QueueListener<T> listener);

    /**
     * 移除消息监听器。使用 {@code equals} 匹配监听器实例。
     *
     * @param listener 要移除的监听器
     */
    void removeListener(@Nonnull QueueListener<T> listener);

    /**
     * 是否开启自动确认（默认 {@code false}）。
     *
     * <p>开启后，所有监听器无异常时自动调用 {@link MessageContext#acknowledge()}，
     * 无需在监听器中显式确认。
     *
     * @return {@code true} 表示已开启自动确认
     */
    boolean isAutoAck();

    /**
     * 设置自动确认模式。
     *
     * @param autoAck {@code true} 开启，{@code false} 关闭
     */
    void setAutoAck(boolean autoAck);

    /**
     * 关闭队列，释放资源。关闭后不能再添加监听器或投递消息。
     * 已启动的消费线程会被中断并退出。
     *
     * @throws IOException 关闭时发生 I/O 错误
     */
    @Override
    void close() throws IOException;
}
