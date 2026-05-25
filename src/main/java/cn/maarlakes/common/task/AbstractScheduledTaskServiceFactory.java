package cn.maarlakes.common.task;

import cn.maarlakes.common.function.Function1;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;

/**
 * 调度任务服务工厂的抽象基类，实现按任务名缓存服务实例的逻辑。
 *
 * <p>核心职责：</p>
 * <ul>
 *     <li>通过 {@link ConcurrentMap} 缓存已创建的服务实例，相同任务名只创建一次</li>
 *     <li>根据 {@link Task} 注解的 {@code value()} 过滤出匹配当前任务名的执行器</li>
 * </ul>
 *
 * <h3>执行器过滤规则</h3>
 * <ul>
 *     <li>执行器类上没有 {@link Task} 注解 → 匹配所有任务</li>
 *     <li>{@link Task#value()} 为空数组 → 匹配所有任务</li>
 *     <li>{@link Task#value()} 非空 → 仅匹配数组中包含的任务名</li>
 * </ul>
 *
 * @author linjpxc
 * @see ScheduledTaskServiceFactory
 * @see Task
 */
public abstract class AbstractScheduledTaskServiceFactory implements ScheduledTaskServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(AbstractScheduledTaskServiceFactory.class);

    /** 按任务名缓存的服务实例 */
    protected final ConcurrentMap<String, ScheduledTaskService<?>> services = new ConcurrentHashMap<>();

    /** 所有已注册的任务执行器 */
    protected final List<? extends TaskExecutor<?>> taskExecutors;

    /** 按任务名创建线程池的工厂函数 */
    protected final Function1<String, Executor> executorFactory;

    /**
     * 使用 {@link ForkJoinPool#commonPool()} 作为默认线程池。
     *
     * @param taskExecutors 所有注册的任务执行器
     */
    protected AbstractScheduledTaskServiceFactory(@Nonnull List<? extends TaskExecutor<?>> taskExecutors) {
        this(taskExecutors, r -> ForkJoinPool.commonPool());
    }

    /**
     * 使用自定义线程池工厂。
     *
     * @param taskExecutors   所有注册的任务执行器
     * @param executorFactory 按任务名创建线程池的工厂函数
     */
    protected AbstractScheduledTaskServiceFactory(@Nonnull List<? extends TaskExecutor<?>> taskExecutors, @Nonnull Function1<String, Executor> executorFactory) {
        this.taskExecutors = taskExecutors;
        this.executorFactory = executorFactory;
    }

    /**
     * 创建或获取指定任务名的调度服务。
     *
     * <p>线程安全：使用 {@code ConcurrentMap.computeIfAbsent} 保证相同任务名只创建一个实例。</p>
     */
    @Override
    @SuppressWarnings("unchecked")
    public final <T> ScheduledTaskService<T> create(@Nonnull String taskName) {
        return (ScheduledTaskService<T>) this.services.computeIfAbsent(taskName, name -> {
            log.info("创建调度任务服务，任务名称：{}", name);
            return this.createService(name);
        });
    }

    /**
     * 根据任务名过滤匹配的任务执行器。
     *
     * <p>过滤规则：</p>
     * <ol>
     *     <li>执行器没有 {@link Task} 注解 → 包含</li>
     *     <li>{@link Task#value()} 为空 → 包含</li>
     *     <li>{@link Task#value()} 包含该任务名 → 包含</li>
     * </ol>
     *
     * @param taskName 任务名称
     * @param <T>      任务数据类型
     * @return 过滤后的执行器列表
     */
    @Nonnull
    @SuppressWarnings("unchecked")
    protected <T> List<? extends TaskExecutor<T>> getTaskExecutors(@Nonnull String taskName) {
        final List<? extends TaskExecutor<T>> filtered = (List<? extends TaskExecutor<T>>) this.taskExecutors.stream().filter(executor -> {
            final Task task = executor.getClass().getAnnotation(Task.class);
            if (task == null) {
                if (log.isTraceEnabled()) {
                    log.trace("执行器 [{}] 无 @Task 注解，匹配所有任务", executor.getClass().getSimpleName());
                }
                return true;
            }
            final String[] value = task.value();
            if (value.length < 1) {
                if (log.isTraceEnabled()) {
                    log.trace("执行器 [{}] @Task.value 为空，匹配所有任务", executor.getClass().getSimpleName());
                }
                return true;
            }
            final boolean match = Arrays.asList(value).contains(taskName);
            if (log.isTraceEnabled()) {
                log.trace("执行器 [{}] @Task.value={}，任务名称 [{}] {}", executor.getClass().getSimpleName(),
                        Arrays.toString(value), taskName, match ? "匹配" : "不匹配");
            }
            return match;
        }).collect(Collectors.toList());
        if (log.isDebugEnabled()) {
            log.debug("任务执行器过滤完成，任务名称：{}，总数：{}，匹配：{}", taskName, this.taskExecutors.size(), filtered.size());
        }
        return filtered;
    }

    /**
     * 模板方法：创建具体的调度任务服务实例。
     *
     * @param taskName 任务名称
     * @param <T>      任务数据类型
     * @return 新创建的调度任务服务
     */
    protected abstract <T> ScheduledTaskService<T> createService(@Nonnull String taskName);
}
