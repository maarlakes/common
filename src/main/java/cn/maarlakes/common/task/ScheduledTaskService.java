package cn.maarlakes.common.task;

import cn.maarlakes.common.utils.Futures;
import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * 调度任务服务，提供任务的延迟调度与生命周期管理。
 *
 * <p>支持两种实现：</p>
 * <ul>
 *     <li>{@link SystemScheduledTaskService} — 基于 JVM 本地 {@code ScheduledExecutorService}，适用于单实例部署</li>
 *     <li>{@code RedissonScheduledTaskService} — 基于 Redis 延迟队列，适用于分布式环境</li>
 * </ul>
 *
 * <p>每个任务通过唯一的 {@link #getTaskName()} 标识，同一任务名的调度请求会被跟踪，
 * 支持取消、去重和查询等生命周期操作。</p>
 *
 * @param <T> 任务数据的类型
 * @author linjpxc
 */
public interface ScheduledTaskService<T> {

    /**
     * 获取任务名称标识。
     *
     * <p>任务名用于工厂的执行器过滤、队列命名以及日志追踪。</p>
     *
     * @return 任务名称
     */
    @Nonnull
    String getTaskName();

    /**
     * 获取当前待执行（等待中）的任务数量。
     *
     * <p>在分布式实现中，此值可能为近似值。</p>
     *
     * @return 待执行任务数量
     */
    int taskCount();

    /**
     * 异步调度一个任务，在指定的延迟后执行。
     *
     * @param task  任务数据
     * @param delay 延迟时间
     * @return 表示调度完成的 {@code CompletionStage}
     */
    CompletionStage<Void> scheduleAsync(@Nonnull T task, @Nonnull Duration delay);

    /**
     * 同步调度一个任务，阻塞等待调度操作完成。
     *
     * @param task  任务数据
     * @param delay 延迟时间
     */
    default void schedule(@Nonnull T task, @Nonnull Duration delay) {
        Futures.await(this.scheduleAsync(task, delay).toCompletableFuture());
    }

    /**
     * 清除所有待执行的任务。
     *
     * <p>已取消的任务不会再被执行。</p>
     */
    void clear();

    /**
     * 取消指定的待执行任务。
     *
     * @param task 要取消的任务数据
     */
    void cancelTask(@Nonnull T task);

    /**
     * 检查指定的任务是否正在等待执行。
     *
     * @param task 要检查的任务数据
     * @return 如果任务在待执行队列中则返回 {@code true}
     */
    boolean containsTask(@Nonnull T task);
}
