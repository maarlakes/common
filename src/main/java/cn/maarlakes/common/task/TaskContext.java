package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;

/**
 * 任务执行上下文，将任务数据与其所属的调度服务绑定在一起。
 *
 * <p>每次任务触发执行时，框架会创建一个 {@code TaskContext} 实例，
 * 将任务数据对象和负责调度的 {@link ScheduledTaskService} 传递给各个 {@link TaskExecutor}。</p>
 *
 * @param <T> 任务数据的类型
 * @author linjpxc
 * @see TaskExecutor#execute(TaskContext)
 */
public interface TaskContext<T> {

    /**
     * 获取拥有该上下文的调度任务服务。
     *
     * @return 调度任务服务实例
     */
    @Nonnull
    ScheduledTaskService<T> getService();

    /**
     * 获取待执行的任务数据。
     *
     * @return 任务数据对象，可能为 {@code null}
     */
    T getTask();
}
