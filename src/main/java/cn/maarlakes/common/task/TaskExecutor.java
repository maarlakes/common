package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;

/**
 * 任务执行器接口，定义单个任务的执行逻辑。
 *
 * <p>每个 {@code TaskExecutor} 实现类负责处理一种特定类型的任务逻辑。
 * 实现类可以通过 {@link Task} 注解声明自己处理哪些任务名称。</p>
 *
 * <p>多个执行器可以处理同一个任务，框架会按 {@code @Order} / {@link cn.maarlakes.common.Ordered}
 * 排序后依次执行。在批量执行时，某个执行器抛出异常不会阻断后续执行器的运行，
 * 所有异常会通过 {@code suppressed} 机制聚合后统一抛出。</p>
 *
 * @param <T> 任务数据的类型
 * @author linjpxc
 * @see Task
 * @see TaskExecutors#execute(java.util.List, TaskContext)
 */
public interface TaskExecutor<T> {

    /**
     * 执行任务。
     *
     * @param context 任务执行上下文，包含任务数据和所属调度服务
     */
    void execute(@Nonnull TaskContext<T> context);
}
