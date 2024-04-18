package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface ScheduledTaskServiceFactory {

    <T> ScheduledTaskService<T> create(@Nonnull String taskName);
}
