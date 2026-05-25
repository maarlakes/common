package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 事件发布器，将事件分发给所有匹配的监听器。
 *
 * <p>提供同步和异步两种发布模式。同步发布在当前线程中执行所有匹配的监听器；
 * 异步发布将整个发布过程（含同步和异步监听器的分发）提交到指定线程池执行。
 *
 * <p><b>注意：</b>即使使用同步发布，标注了 {@code @EventDispatch(async = true)} 的监听器
 * 仍会被异步执行，但 {@link #publish(Object)} 会等待所有异步监听器完成后才返回。
 *
 * @author linjpxc
 * @see DefaultEventPublisher
 */
public interface EventPublisher {

    /**
     * 同步发布事件。
     *
     * <p>将事件分发给所有匹配的监听器。同步监听器在当前线程中依次执行，
     * 异步监听器（标注了 {@code @EventDispatch(async = true)}）提交到线程池执行，
     * 本方法会等待所有异步监听器执行完毕后才返回。
     *
     * @param <E>   事件类型
     * @param event 要发布的事件对象，不能为 null
     */
    <E> void publish(@Nonnull E event);

    /**
     * 异步发布事件，使用 {@link ForkJoinPool#commonPool()} 作为执行器。
     *
     * <p>整个发布过程（包含对 {@link #publish(Object)} 的调用）将在公共线程池中执行，
     * 调用方可通过返回的 {@link CompletionStage} 感知发布完成时机。
     *
     * @param <E>   事件类型
     * @param event 要发布的事件对象，不能为 null
     * @return 表示异步发布任务的 {@link CompletionStage}
     */
    @Nonnull
    default <E> CompletionStage<Void> publishAsync(@Nonnull E event) {
        return this.publishAsync(event, ForkJoinPool.commonPool());
    }

    /**
     * 异步发布事件，使用指定的执行器。
     *
     * @param <E>       事件类型
     * @param event     要发布的事件对象，不能为 null
     * @param executor  用于执行发布任务的线程池，不能为 null
     * @return 表示异步发布任务的 {@link CompletionStage}
     */
    @Nonnull
    default <E> CompletionStage<Void> publishAsync(@Nonnull E event, @Nonnull Executor executor) {
        return CompletableFuture.runAsync(() -> this.publish(event), executor);
    }
}
