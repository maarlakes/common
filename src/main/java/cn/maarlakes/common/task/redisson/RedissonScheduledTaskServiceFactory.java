package cn.maarlakes.common.task.redisson;

import cn.maarlakes.common.function.Function1;
import cn.maarlakes.common.task.AbstractScheduledTaskServiceFactory;
import cn.maarlakes.common.task.ScheduledTaskService;
import cn.maarlakes.common.task.TaskExecutor;
import jakarta.annotation.Nonnull;
import org.redisson.api.RedissonClient;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;

/**
 * @author linjpxc
 */
public class RedissonScheduledTaskServiceFactory extends AbstractScheduledTaskServiceFactory {

    private final RedissonClient redissonClient;
    private final String namespace;

    public RedissonScheduledTaskServiceFactory(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull List<? extends TaskExecutor<?>> taskExecutors) {
        this(redissonClient, namespace, taskExecutors, (taskName) -> new ForkJoinPool());
    }

    public RedissonScheduledTaskServiceFactory(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, List<? extends TaskExecutor<?>> taskExecutors, @Nonnull Function1<String, Executor> executorFactory) {
        super(taskExecutors, executorFactory);
        this.redissonClient = redissonClient;
        this.namespace = namespace;
    }

    @Override
    @SuppressWarnings({"rawtypes", "unchecked"})
    protected <T> ScheduledTaskService<T> createService(@Nonnull String taskName) {
        return new RedissonScheduledTaskService(this.redissonClient, this.namespace, taskName, this.getTaskExecutors(taskName), this.executorFactory.apply(taskName));
    }

}
