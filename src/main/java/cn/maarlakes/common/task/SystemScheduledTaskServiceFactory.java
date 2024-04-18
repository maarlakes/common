package cn.maarlakes.common.task;

import cn.maarlakes.common.function.Function1;
import cn.maarlakes.common.utils.NamedThreadFactory;
import jakarta.annotation.Nonnull;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * @author linjpxc
 */
public class SystemScheduledTaskServiceFactory extends AbstractScheduledTaskServiceFactory {

    public SystemScheduledTaskServiceFactory(@Nonnull List<? extends TaskExecutor<?>> taskExecutors) {
        super(taskExecutors);
    }

    public SystemScheduledTaskServiceFactory(List<? extends TaskExecutor<?>> taskExecutors, @Nonnull Function1<String, Executor> executorFactory) {
        super(taskExecutors, executorFactory);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T> ScheduledTaskService<T> createService(@Nonnull String taskName) {
        return new SystemScheduledTaskService(taskName, this.createScheduledTaskService(taskName), this.getTaskExecutors(taskName), this.executorFactory.apply(taskName));
    }

    @Nonnull
    protected ScheduledExecutorService createScheduledTaskService(@Nonnull String taskName) {
        return new ScheduledThreadPoolExecutor(Runtime.getRuntime().availableProcessors(), new NamedThreadFactory("system-scheduled-task-" + taskName + "-"));
    }
}
