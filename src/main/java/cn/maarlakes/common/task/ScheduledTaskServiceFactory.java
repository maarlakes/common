package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;

/**
 * 调度任务服务工厂，按任务名创建或获取缓存的 {@link ScheduledTaskService} 实例。
 *
 * <p>实现类通过 {@code ConcurrentMap} 缓存已创建的服务实例，
 * 相同的任务名总是返回同一个服务实例（{@code computeIfAbsent} 语义）。</p>
 *
 * @author linjpxc
 * @see AbstractScheduledTaskServiceFactory
 */
public interface ScheduledTaskServiceFactory {

    /**
     * 创建或获取指定任务名的调度服务。
     *
     * <p>首次调用时创建新实例并缓存，后续调用直接返回缓存实例。</p>
     *
     * @param taskName 任务名称
     * @param <T>      任务数据的类型
     * @return 调度任务服务实例
     */
    <T> ScheduledTaskService<T> create(@Nonnull String taskName);
}
