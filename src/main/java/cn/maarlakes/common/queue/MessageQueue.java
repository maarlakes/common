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
 * <p>推模式：通过 {@link #addListener} 注册监听器，队列内部启动消费线程自动获取消息并分发。
 * 拉模式：通过 {@link #poll} / {@link #pollAsync} 主动拉取消息。
 *
 * <p>消息确认机制：监听器收到消息后，可通过 {@link MessageContext#acknowledge()} 确认消息。
 * 未确认的消息会在处理完成后重新入队。开启 {@link #setAutoAck(boolean) autoAck} 后，
 * 监听器无异常时自动确认。
 *
 * @author linjpxc
 */
public interface MessageQueue<T> extends AutoCloseable {

    /** 队列名称 */
    @Nonnull
    String name();

    /** 队列中的消息数量 */
    int size();

    /** 异步获取队列大小 */
    CompletionStage<Integer> sizeAsync();

    /** 队列是否为空 */
    boolean isEmpty();

    /** 异步判断队列是否为空 */
    CompletionStage<Boolean> isEmptyAsync();

    /** 投递消息到队列 */
    boolean offer(@Nonnull T value);

    /** 异步投递消息 */
    CompletionStage<Boolean> offerAsync(@Nonnull T value);

    /** 拉取并移除队首消息，队列为空时返回 null */
    T poll();

    /** 异步拉取队首消息 */
    CompletionStage<T> pollAsync();

    /** 清空队列 */
    void clear();

    /** 异步清空队列 */
    CompletionStage<Void> clearAsync();

    /** 从队列中移除指定消息 */
    boolean remove(@Nonnull T value);

    /** 异步移除指定消息 */
    CompletionStage<Boolean> removeAsync(@Nonnull T value);

    /** 从队列中批量移除消息 */
    boolean removeAll(@Nonnull Collection<? extends T> values);

    /** 异步批量移除消息 */
    CompletionStage<Boolean> removeAllAsync(@Nonnull Collection<? extends T> values);

    /** 按条件移除消息，返回被移除的消息列表 */
    List<? extends T> removeIf(@Nonnull Predicate<T> predicate);

    /** 队列是否包含指定消息 */
    boolean contains(@Nonnull T value);

    /** 异步判断是否包含指定消息 */
    CompletionStage<Boolean> containsAsync(@Nonnull T value);

    /**
     * 添加消息监听器。首个监听器添加后，消费线程自动启动。
     *
     * @throws IllegalStateException 队列已关闭
     */
    void addListener(@Nonnull QueueListener<T> listener);

    /** 移除消息监听器 */
    void removeListener(@Nonnull QueueListener<T> listener);

    /** 是否开启自动确认（默认 false）。开启后监听器无异常时自动 ack */
    boolean isAutoAck();

    /** 设置自动确认模式 */
    void setAutoAck(boolean autoAck);

    /** 关闭队列，释放资源 */
    @Override
    void close() throws IOException;
}
