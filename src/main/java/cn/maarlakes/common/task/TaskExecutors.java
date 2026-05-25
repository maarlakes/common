package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 任务执行器批量执行工具类。
 *
 * <p>提供两种执行模式：</p>
 * <ul>
 *     <li><strong>同步执行</strong>：{@link #execute} 方法按顺序依次调用每个执行器，
 *     某个执行器抛出异常不会阻断后续执行器的运行，所有异常通过 {@code suppressed} 机制聚合后统一抛出</li>
 *     <li><strong>异步执行</strong>：{@link #executeAsync} 方法将同步执行包装在 {@code CompletableFuture} 中，
 *     在指定线程池上异步运行</li>
 * </ul>
 *
 * @author linjpxc
 * @see TaskExecutor
 */
public final class TaskExecutors {
    private static final Logger log = LoggerFactory.getLogger(TaskExecutors.class);

    private TaskExecutors() {
    }

    /**
     * 同步执行所有任务执行器。
     *
     * <p>按顺序依次调用每个执行器的 {@link TaskExecutor#execute} 方法。
     * 如果某个执行器抛出异常，会继续执行后续执行器，最后将所有异常聚合后抛出。
     * 第一个异常作为主异常，后续异常通过 {@code addSuppressed} 附加。</p>
     *
     * @param executors 任务执行器列表
     * @param context   任务执行上下文
     * @param <T>       任务数据类型
     */
    public static <T> void execute(@Nonnull List<? extends TaskExecutor<T>> executors, @Nonnull TaskContext<T> context) {
        if (log.isTraceEnabled()) {
            log.trace("开始执行任务，任务名称：{}，执行器数量：{}", context.getService().getTaskName(), executors.size());
        }
        RuntimeException firstError = null;
        for (TaskExecutor<T> executor : executors) {
            try {
                executor.execute(context);
            } catch (RuntimeException e) {
                if (log.isWarnEnabled()) {
                    log.warn("任务执行器 [{}] 执行异常，任务名称：{}", executor.getClass().getSimpleName(), context.getService().getTaskName(), e);
                }
                if (firstError == null) {
                    firstError = e;
                } else {
                    firstError.addSuppressed(e);
                }
            }
        }
        if (firstError != null) {
            log.error("任务执行完成但有异常，任务名称：{}，异常数量：{}", context.getService().getTaskName(),
                    1 + firstError.getSuppressed().length);
            throw firstError;
        }
    }

    /**
     * 使用 {@link ForkJoinPool#commonPool()} 异步执行所有任务执行器。
     *
     * @param taskExecutors 任务执行器列表
     * @param context       任务执行上下文
     * @param <T>           任务数据类型
     * @return 表示执行完成的 {@code CompletionStage}
     */
    public static <T> CompletionStage<Void> executeAsync(@Nonnull List<? extends TaskExecutor<T>> taskExecutors, @Nonnull TaskContext<T> context) {
        return executeAsync(taskExecutors, context, ForkJoinPool.commonPool());
    }

    /**
     * 在指定线程池上异步执行所有任务执行器。
     *
     * @param taskExecutors 任务执行器列表
     * @param context       任务执行上下文
     * @param executor      线程池
     * @param <T>           任务数据类型
     * @return 表示执行完成的 {@code CompletionStage}
     */
    public static <T> CompletionStage<Void> executeAsync(@Nonnull List<? extends TaskExecutor<T>> taskExecutors, @Nonnull TaskContext<T> context, @Nonnull Executor executor) {
        return CompletableFuture.runAsync(() -> execute(taskExecutors, context), executor);
    }
}
