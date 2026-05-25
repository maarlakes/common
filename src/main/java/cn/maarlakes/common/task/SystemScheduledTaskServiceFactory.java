package cn.maarlakes.common.task;

import cn.maarlakes.common.function.Function1;
import cn.maarlakes.common.utils.NamedThreadFactory;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

/**
 * 基于 JVM 本地 {@link ScheduledExecutorService} 的调度任务服务工厂。
 *
 * <p>为每个任务名创建独立的 {@link SystemScheduledTaskService} 实例。
 * 每个实例使用单线程 {@code ScheduledThreadPoolExecutor}，线程名格式为
 * {@code system-scheduled-task-{taskName}-}，便于在日志和线程转储中识别。</p>
 *
 * @author linjpxc
 * @see SystemScheduledTaskService
 */
public class SystemScheduledTaskServiceFactory extends AbstractScheduledTaskServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(SystemScheduledTaskServiceFactory.class);

    /**
     * 使用 {@link java.util.concurrent.ForkJoinPool#commonPool()} 作为默认执行线程池。
     *
     * @param taskExecutors 所有注册的任务执行器
     */
    public SystemScheduledTaskServiceFactory(@Nonnull List<? extends TaskExecutor<?>> taskExecutors) {
        super(taskExecutors);
    }

    /**
     * 使用自定义线程池工厂。
     *
     * @param taskExecutors   所有注册的任务执行器
     * @param executorFactory 按任务名创建线程池的工厂函数
     */
    public SystemScheduledTaskServiceFactory(List<? extends TaskExecutor<?>> taskExecutors, @Nonnull Function1<String, Executor> executorFactory) {
        super(taskExecutors, executorFactory);
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T> ScheduledTaskService<T> createService(@Nonnull String taskName) {
        log.info("创建系统调度任务服务，任务名称：{}", taskName);
        return new SystemScheduledTaskService(taskName, this.createScheduledTaskService(taskName), this.getTaskExecutors(taskName), this.executorFactory.apply(taskName));
    }

    /**
     * 为指定任务名创建单线程调度器。
     *
     * <p>线程名格式为 {@code system-scheduled-task-{taskName}-}。</p>
     *
     * @param taskName 任务名称
     * @return 调度器实例
     */
    @Nonnull
    protected ScheduledExecutorService createScheduledTaskService(@Nonnull String taskName) {
        return new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("system-scheduled-task-" + taskName + "-"));
    }
}
