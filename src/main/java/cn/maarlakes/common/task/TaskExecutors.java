package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * @author linjpxc
 */
public final class TaskExecutors {
    private TaskExecutors() {
    }

    public static <T> void execute(@Nonnull List<? extends TaskExecutor<T>> executors, @Nonnull TaskContext<T> context) {
        executors.forEach(executor -> executor.execute(context));
    }

    public static <T> CompletionStage<Void> executeAsync(@Nonnull List<? extends TaskExecutor<T>> taskExecutors, @Nonnull TaskContext<T> context) {
        return executeAsync(taskExecutors, context, ForkJoinPool.commonPool());
    }

    public static <T> CompletionStage<Void> executeAsync(@Nonnull List<? extends TaskExecutor<T>> taskExecutors, @Nonnull TaskContext<T> context, @Nonnull Executor executor) {
        return CompletableFuture.runAsync(() -> execute(taskExecutors, context), executor);
    }
}
