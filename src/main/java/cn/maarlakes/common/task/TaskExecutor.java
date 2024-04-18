package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;

/**
 * @author linjpxc
 */
public interface TaskExecutor<T> {

    void execute(@Nonnull TaskContext<T> context);
}
