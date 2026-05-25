package cn.maarlakes.common.task;

import cn.maarlakes.common.AnnotationOrderComparator;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;

/**
 * 调度任务服务的抽象基类，封装了通用的执行器管理和异步执行管道。
 *
 * <p>执行管道：</p>
 * <ol>
 *     <li>子类通过 {@link #scheduleAsync} 接收任务，存入待执行集合</li>
 *     <li>延迟到期后调用 {@link #executeTask} 触发执行</li>
 *     <li>执行器按 {@code @Order} / {@link AnnotationOrderComparator} 排序后依次执行</li>
 *     <li>通过 {@link TaskExecutors#executeAsync} 在 {@link #executor} 上异步运行</li>
 *     <li>执行异常通过 {@code whenComplete} 记录日志</li>
 * </ol>
 *
 * <p>子类需实现 {@link #log()} 方法提供各自的日志器，以及 {@link ScheduledTaskService} 的调度相关方法。</p>
 *
 * @param <T> 任务数据的类型
 * @author linjpxc
 * @see SystemScheduledTaskService
 * @see TaskExecutors
 */
public abstract class AbstractScheduledTaskService<T> implements ScheduledTaskService<T> {

    /** 按 {@code @Order} 排序后的任务执行器列表 */
    protected final List<? extends TaskExecutor<T>> taskExecutors;

    /** 异步执行任务时使用的线程池 */
    protected final Executor executor;

    /**
     * 创建调度任务服务实例。
     *
     * <p>执行器列表会通过 {@link AnnotationOrderComparator} 排序后保存。</p>
     *
     * @param taskExecutors 任务执行器列表
     * @param executor      异步执行线程池
     */
    protected AbstractScheduledTaskService(@Nonnull List<? extends TaskExecutor<T>> taskExecutors, @Nonnull Executor executor) {
        this.executor = executor;
        this.taskExecutors = taskExecutors.stream().sorted(AnnotationOrderComparator.getInstance()).collect(Collectors.toList());
    }

    /**
     * 异步执行任务，将任务分发给所有匹配的执行器。
     *
     * <p>执行通过 {@link TaskExecutors#executeAsync} 在 {@link #executor} 上异步完成，
     * 执行异常会通过 {@code whenComplete} 回调记录到日志。</p>
     *
     * @param task 任务数据
     */
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

    /**
     * 获取子类的日志器实例。
     *
     * @return 日志器
     */
    @Nonnull
    protected abstract Logger log();

    /**
     * 默认的 {@link TaskContext} 实现，绑定任务数据与其所属的调度服务。
     */
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
