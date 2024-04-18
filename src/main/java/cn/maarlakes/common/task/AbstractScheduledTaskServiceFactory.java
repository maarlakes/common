package cn.maarlakes.common.task;

import cn.maarlakes.common.function.Function1;
import jakarta.annotation.Nonnull;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public abstract class AbstractScheduledTaskServiceFactory implements ScheduledTaskServiceFactory {

    protected final ConcurrentMap<String, ScheduledTaskService<?>> services = new ConcurrentHashMap<>();
    protected final List<? extends TaskExecutor<?>> taskExecutors;
    protected final Function1<String, Executor> executorFactory;

    protected AbstractScheduledTaskServiceFactory(@Nonnull List<? extends TaskExecutor<?>> taskExecutors) {
        this(taskExecutors, r -> new ForkJoinPool());
    }

    protected AbstractScheduledTaskServiceFactory(@Nonnull List<? extends TaskExecutor<?>> taskExecutors, @Nonnull Function1<String, Executor> executorFactory) {
        this.taskExecutors = taskExecutors;
        this.executorFactory = executorFactory;
    }

    @Override
    @SuppressWarnings("unchecked")
    public final <T> ScheduledTaskService<T> create(@Nonnull String taskName) {
        return (ScheduledTaskService<T>) this.services.computeIfAbsent(taskName, this::createService);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    protected <T> List<? extends TaskExecutor<T>> getTaskExecutors(@Nonnull String taskName) {
        return (List<? extends TaskExecutor<T>>) this.taskExecutors.stream().filter(executor -> {
            final Task task = executor.getClass().getAnnotation(Task.class);
            if (task == null) {
                return true;
            }
            final String[] value = task.value();
            if (value.length < 1) {
                return true;
            }
            return Arrays.asList(value).contains(taskName);
        }).collect(Collectors.toList());
    }

    protected abstract <T> ScheduledTaskService<T> createService(@Nonnull String taskName);
}
