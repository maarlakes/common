package cn.maarlakes.common.task;

import cn.maarlakes.common.utils.Futures;
import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.util.concurrent.CompletionStage;

/**
 * @author linjpxc
 */
public interface ScheduledTaskService<T> {

    @Nonnull
    String getTaskName();

    int taskCount();

    CompletionStage<Void> scheduleAsync(@Nonnull T task, @Nonnull Duration delay);

    default void schedule(@Nonnull T task, @Nonnull Duration delay) {
        Futures.await(this.scheduleAsync(task, delay).toCompletableFuture());
    }

    void clear();

    void cancelTask(@Nonnull T task);

    boolean containsTask(@Nonnull T task);
}
