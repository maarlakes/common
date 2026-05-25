package cn.maarlakes.common.task.redisson;

import cn.maarlakes.common.function.Function1;
import cn.maarlakes.common.task.AbstractScheduledTaskServiceFactory;
import cn.maarlakes.common.task.ScheduledTaskService;
import cn.maarlakes.common.task.TaskExecutor;
import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * 基于 Redisson 的分布式调度任务服务工厂。
 *
 * <p>为每个任务名创建 {@link RedissonScheduledTaskService} 实例，
 * 使用指定的 Redis 命名空间作为队列 key 前缀。</p>
 *
 * <p>队列命名规则：</p>
 * <ul>
 *     <li>命名空间以 {@code :} 结尾时：{@code namespace + taskName}</li>
 *     <li>否则：{@code namespace + ":" + taskName}</li>
 * </ul>
 *
 * @author linjpxc
 * @see RedissonScheduledTaskService
 */
public class RedissonScheduledTaskServiceFactory extends AbstractScheduledTaskServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(RedissonScheduledTaskServiceFactory.class);

    /** Redisson 客户端实例 */
    private final RedissonClient redissonClient;

    /** Redis key 命名空间前缀 */
    private final String namespace;

    /**
     * 创建 Redisson 调度任务服务工厂，每个任务使用独立的 {@link ForkJoinPool} 执行。
     *
     * @param redissonClient Redisson 客户端
     * @param namespace      Redis key 命名空间前缀
     * @param taskExecutors  所有注册的任务执行器
     */
    public RedissonScheduledTaskServiceFactory(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull List<? extends TaskExecutor<?>> taskExecutors) {
        this(redissonClient, namespace, taskExecutors, (taskName) -> new ForkJoinPool());
    }

    /**
     * 创建 Redisson 调度任务服务工厂，使用自定义线程池工厂。
     *
     * @param redissonClient  Redisson 客户端
     * @param namespace       Redis key 命名空间前缀
     * @param taskExecutors   所有注册的任务执行器
     * @param executorFactory 按任务名创建线程池的工厂函数
     */
    public RedissonScheduledTaskServiceFactory(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, List<? extends TaskExecutor<?>> taskExecutors, @Nonnull Function1<String, Executor> executorFactory) {
        super(taskExecutors, executorFactory);
        this.redissonClient = redissonClient;
        this.namespace = namespace;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T> ScheduledTaskService<T> createService(@Nonnull String taskName) {
        log.info("创建 Redisson 调度任务服务，任务名称：{}，命名空间：{}", taskName, this.namespace);
        return new RedissonScheduledTaskService(this.redissonClient, this.namespace, taskName, this.getTaskExecutors(taskName), this.executorFactory.apply(taskName));
    }

}
