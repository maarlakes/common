package cn.maarlakes.common.task;

import cn.maarlakes.common.AnnotationOrderComparator;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public abstract class AbstractScheduledTaskService<T> implements ScheduledTaskService<T> {

    protected final List<? extends TaskExecutor<T>> taskExecutors;
    protected final Executor executor;

    protected AbstractScheduledTaskService(@Nonnull List<? extends TaskExecutor<T>> taskExecutors, @Nonnull Executor executor) {
        this.executor = executor;
        this.taskExecutors = taskExecutors.stream().sorted(AnnotationOrderComparator.getInstance()).collect(Collectors.toList());
    }

    protected void executeTask(T task) {
        final Logger log = this.log();
        if (log.isDebugEnabled()) {
            log.debug("任务调度，任务名称：{}，任务内容：{}", this.getTaskName(), task);
        }
        TaskExecutors.executeAsync(this.taskExecutors, new DefaultTaskContext<T>(this, task), this.executor)
                .whenComplete((r, error) -> {
                    if (error != null && log.isErrorEnabled()) {
                        log.error("任务执行异常，任务名称：{}，任务内容：{}", this.getTaskName(), task, error);
                    }
                });
    }

    @Nonnull
    protected abstract Logger log();

    protected static class DefaultTaskContext<T> implements TaskContext<T> {

        private final ScheduledTaskService<T> service;
        private final T task;

        public DefaultTaskContext(@Nonnull ScheduledTaskService<T> service, @Nonnull T task) {
            this.service = service;
            this.task = task;
        }

        @Nonnull
        @Override
        public ScheduledTaskService<T> getService() {
            return this.service;
        }

        @Override
        public T getTask() {
            return this.task;
        }
    }
}
