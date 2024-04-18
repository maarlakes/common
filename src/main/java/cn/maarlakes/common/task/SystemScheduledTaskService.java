package cn.maarlakes.common.task;

import cn.maarlakes.common.utils.NamedThreadFactory;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author linjpxc
 */
public class SystemScheduledTaskService<T> extends AbstractScheduledTaskService<T> {
    private static final Logger log = LoggerFactory.getLogger(SystemScheduledTaskService.class);

    private final String taskName;
    private final ScheduledExecutorService taskScheduler;
    private final ConcurrentMap<T, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    public SystemScheduledTaskService(@Nonnull String taskName, @Nonnull List<? extends TaskExecutor<T>> taskExecutors) {
        this(taskName, new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new NamedThreadFactory("schedule-task-" + taskName + "-")), taskExecutors, new ForkJoinPool());
    }

    public SystemScheduledTaskService(@Nonnull String taskName, @Nonnull ScheduledExecutorService taskScheduler, @Nonnull List<? extends TaskExecutor<T>> taskExecutors) {
        this(taskName, taskScheduler, taskExecutors, taskScheduler);
    }

    public SystemScheduledTaskService(@Nonnull String taskName, @Nonnull ScheduledExecutorService taskScheduler, @Nonnull List<? extends TaskExecutor<T>> taskExecutors, @Nonnull Executor executor) {
        super(taskExecutors, executor);
        this.taskName = taskName;
        this.taskScheduler = taskScheduler;
    }

    @Nonnull
    @Override
    public String getTaskName() {
        return this.taskName;
    }

    @Override
    public int taskCount() {
        return this.tasks.size();
    }

    @Override
    public CompletionStage<Void> scheduleAsync(@Nonnull T task, @Nonnull Duration delay) {
        this.schedule(task, delay);
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void schedule(@Nonnull T task, @Nonnull Duration delay) {
        this.tasks.merge(
                task,
                this.taskScheduler.schedule(() -> {
                    this.tasks.remove(task);
                    this.executeTask(task);
                }, delay.toMillis(), TimeUnit.MILLISECONDS),
                (oldFuture, future) -> {
                    oldFuture.cancel(true);
                    return future;
                }
        );
    }

    @Override
    public void clear() {
        final Set<T> keys = this.tasks.keySet();
        for (T key : keys) {
            final ScheduledFuture<?> future = this.tasks.remove(key);
            if (future != null && !future.isDone()) {
                future.cancel(true);
            }
        }
    }

    @Override
    public void cancelTask(@Nonnull T task) {
        final ScheduledFuture<?> future = this.tasks.remove(task);
        if (future != null) {
            future.cancel(true);
        }
    }

    @Override
    public boolean containsTask(@Nonnull T task) {
        return this.tasks.containsKey(task);
    }

    @Nonnull
    @Override
    protected Logger log() {
        return log;
    }
}
