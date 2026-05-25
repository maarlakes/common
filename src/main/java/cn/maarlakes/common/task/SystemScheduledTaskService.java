package cn.maarlakes.common.task;

import cn.maarlakes.common.utils.NamedThreadFactory;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * 基于 JVM 本地 {@link ScheduledExecutorService} 的调度任务服务实现。
 *
 * <p>使用单线程 {@code ScheduledThreadPoolExecutor} 管理延迟调度，
 * 通过 {@link ConcurrentMap} 跟踪所有待执行的任务。</p>
 *
 * <h3>重调度行为</h3>
 * <p>如果对同一个任务对象再次调用 {@link #schedule}，之前的调度会被取消并由新的延迟替代。
 * 这意味着同一任务对象在任意时刻最多只有一个待执行的调度。</p>
 *
 * <p>适用于单实例部署；分布式环境请使用 {@code RedissonScheduledTaskService}。</p>
 *
 * @param <T> 任务数据的类型
 * @author linjpxc
 * @see AbstractScheduledTaskService
 */
public class SystemScheduledTaskService<T> extends AbstractScheduledTaskService<T> {
    private static final Logger log = LoggerFactory.getLogger(SystemScheduledTaskService.class);

    /** 任务名称标识 */
    private final String taskName;

    /** 单线程调度器，负责延迟计时 */
    private final ScheduledExecutorService taskScheduler;

    /** 任务对象到其调度未来的映射，用于跟踪和取消 */
    private final ConcurrentMap<T, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();

    /**
     * 创建系统调度任务服务，使用默认的单线程调度器。
     *
     * <p>线程名格式为 {@code schedule-task-{taskName}-}。</p>
     *
     * @param taskName      任务名称
     * @param taskExecutors 任务执行器列表
     */
    public SystemScheduledTaskService(@Nonnull String taskName, @Nonnull List<? extends TaskExecutor<T>> taskExecutors) {
        this(taskName, new ScheduledThreadPoolExecutor(1, new NamedThreadFactory("schedule-task-" + taskName + "-")), taskExecutors, ForkJoinPool.commonPool());
    }

    /**
     * 创建系统调度任务服务，使用自定义调度器，执行器与调度器共用同一线程池。
     *
     * @param taskName      任务名称
     * @param taskScheduler 调度器
     * @param taskExecutors 任务执行器列表
     */
    public SystemScheduledTaskService(@Nonnull String taskName, @Nonnull ScheduledExecutorService taskScheduler, @Nonnull List<? extends TaskExecutor<T>> taskExecutors) {
        this(taskName, taskScheduler, taskExecutors, taskScheduler);
    }

    /**
     * 创建系统调度任务服务，调度器和执行线程池独立配置。
     *
     * @param taskName      任务名称
     * @param taskScheduler 调度器（用于延迟计时）
     * @param taskExecutors 任务执行器列表
     * @param executor      异步执行线程池（用于实际执行任务逻辑）
     */
    public SystemScheduledTaskService(@Nonnull String taskName, @Nonnull ScheduledExecutorService taskScheduler, @Nonnull List<? extends TaskExecutor<T>> taskExecutors, @Nonnull Executor executor) {
        super(taskExecutors, executor);
        this.taskName = taskName;
        this.taskScheduler = taskScheduler;
    }

    @Nonnull
    @Override
    public String getTaskName() {
        return this.taskName;
    }

    @Override
    public int taskCount() {
        return this.tasks.size();
    }

    @Override
    public CompletionStage<Void> scheduleAsync(@Nonnull T task, @Nonnull Duration delay) {
        this.schedule(task, delay);
        return CompletableFuture.completedFuture(null);
    }

    /**
     * 调度任务，在指定延迟后执行。
     *
     * <p>使用 {@link ConcurrentMap#merge} 语义：如果同一任务对象已有待执行的调度，
     * 会取消旧调度并替换为新延迟。</p>
     *
     * @param task  任务数据
     * @param delay 延迟时间
     */
    @Override
    public void schedule(@Nonnull T task, @Nonnull Duration delay) {
        if (log.isDebugEnabled()) {
            log.debug("调度任务 [{}] 延迟 {}ms，任务内容：{}", this.taskName, delay.toMillis(), task);
        }
        this.tasks.merge(
                task,
                this.taskScheduler.schedule(() -> {
                    this.tasks.remove(task);
                    this.executeTask(task);
                }, delay.toMillis(), TimeUnit.MILLISECONDS),
                (oldFuture, future) -> {
                    // 取消旧的调度，用新的替代
                    oldFuture.cancel(true);
                    return future;
                }
        );
    }

    /**
     * 清除所有待执行的任务。
     *
     * <p>遍历所有待执行任务，逐一取消其调度。使用 {@code keySet()} 遍历
     * 配合 {@code remove()} 避免 {@code ConcurrentModificationException}。</p>
     */
    @Override
    public void clear() {
        final Set<T> keys = this.tasks.keySet();
        int count = 0;
        for (T key : keys) {
            final ScheduledFuture<?> future = this.tasks.remove(key);
            if (future != null && !future.isDone()) {
                future.cancel(true);
                count++;
            }
        }
        log.info("清除任务 [{}] 的所有待执行调度，取消数量：{}", this.taskName, count);
    }

    @Override
    public void cancelTask(@Nonnull T task) {
        final ScheduledFuture<?> future = this.tasks.remove(task);
        if (future != null) {
            future.cancel(true);
            if (log.isDebugEnabled()) {
                log.debug("取消任务 [{}] 的调度，任务内容：{}", this.taskName, task);
            }
        }
    }

    @Override
    public boolean containsTask(@Nonnull T task) {
        return this.tasks.containsKey(task);
    }

    @Nonnull
    @Override
    protected Logger log() {
        return log;
    }
}
