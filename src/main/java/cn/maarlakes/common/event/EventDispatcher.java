package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;

/**
 * 事件分发器，负责将事件分发给具体的监听器执行。
 *
 * <p>提供同步和异步两种分发模式。同步分发在当前线程中执行监听器方法；
 * 异步分发通过线程池提交任务，返回 {@link CompletableFuture} 以便调用方感知完成状态。
 *
 * @author linjpxc
 * @see DefaultEventDispatcher
 */
public interface EventDispatcher {

    /**
     * 同步分发事件到指定监听器。
     *
     * @param <E>     事件类型
     * @param invoker 事件调用器，封装了监听器方法和目标对象
     * @param event   待分发的事件对象
     */
    <E> void dispatch(@Nonnull EventInvoker invoker, @Nonnull E event);

    /**
     * 异步分发事件到指定监听器。
     *
     * @param <E>     事件类型
     * @param invoker 事件调用器，封装了监听器方法和目标对象
     * @param event   待分发的事件对象
     * @return 表示异步分发任务的 {@link CompletableFuture}，分发完成或异常时结束
     */
    <E> CompletableFuture<Void> dispatchAsync(@Nonnull EventInvoker invoker, @Nonnull E event);
}
