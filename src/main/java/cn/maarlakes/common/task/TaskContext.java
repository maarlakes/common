package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface TaskContext<T> {

    @Nonnull
    ScheduledTaskService<T> getService();

    T getTask();
}
