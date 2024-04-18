package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;

import java.io.Serializable;

/**
 * @author linjpxc
 */
public interface TaskContext<T> extends Serializable {

    @Nonnull
    ScheduledTaskService<T> getService();

    T getTask();
}
