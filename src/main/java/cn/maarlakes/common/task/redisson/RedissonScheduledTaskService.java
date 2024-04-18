package cn.maarlakes.common.task.redisson;

import cn.maarlakes.common.task.AbstractScheduledTaskService;
import cn.maarlakes.common.task.SystemScheduledTaskService;
import cn.maarlakes.common.task.TaskExecutor;
import jakarta.annotation.Nonnull;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RFuture;
import org.redisson.api.RedissonClient;
import org.redisson.codec.Kryo5Codec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * @author linjpxc
 */
public class RedissonScheduledTaskService<T> extends AbstractScheduledTaskService<T> implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(SystemScheduledTaskService.class);

    private final RDelayedQueue<T> delayedQueue;
    private final RBlockingQueue<T> blockingQueue;
    private final String taskName;
    private boolean isRunning = true;

    public RedissonScheduledTaskService(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull String taskName, @Nonnull List<? extends TaskExecutor<T>> taskExecutors) {
        this(redissonClient, namespace, taskName, taskExecutors, new ForkJoinPool());
    }

    public RedissonScheduledTaskService(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull String taskName, @Nonnull List<? extends TaskExecutor<T>> taskExecutors, @Nonnull Executor executor) {
        super(taskExecutors, executor);
        final Kryo5Codec codec = new Kryo5Codec();
        this.blockingQueue = redissonClient.getBlockingQueue(namespace.endsWith(":") ? namespace + taskName : namespace + ":" + taskName, codec);
        this.delayedQueue = redissonClient.getDelayedQueue(this.blockingQueue);

        this.taskName = taskName;
        final Thread executeThread = new Thread(() -> this.take(redissonClient), "redisson-schedule-task-daemon");
        executeThread.setDaemon(true);
        executeThread.start();
    }

    @Nonnull
    @Override
    public String getTaskName() {
        return this.taskName;
    }

    @Override
    public int taskCount() {
        final RFuture<Integer> delayedFuture = this.delayedQueue.sizeAsync();
        final RFuture<Integer> queueFuture = this.blockingQueue.sizeAsync();

        return Optional.ofNullable(delayedFuture.toCompletableFuture().join()).orElse(0)
                + Optional.ofNullable(queueFuture.toCompletableFuture().join()).orElse(0);
    }

    @Override
    public CompletionStage<Void> scheduleAsync(@Nonnull T task, @Nonnull Duration delay) {
        return CompletableFuture.allOf(
                this.delayedQueue.removeAsync(task).toCompletableFuture(),
                this.blockingQueue.removeAsync(task).toCompletableFuture()
        ).thenCompose(v -> this.delayedQueue.offerAsync(task, delay.toMillis(), TimeUnit.MILLISECONDS).toCompletableFuture());
    }

    @Override
    public void clear() {
        this.delayedQueue.clear();
        this.blockingQueue.clear();
    }

    @Override
    public void cancelTask(@Nonnull T task) {
        CompletableFuture.allOf(
                this.delayedQueue.removeAsync(task).toCompletableFuture(),
                this.blockingQueue.removeAsync(task).toCompletableFuture()
        ).join();
    }

    @Override
    public boolean containsTask(@Nonnull T task) {
        final CompletableFuture<Boolean> delayedFuture = this.delayedQueue.containsAsync(task).toCompletableFuture();
        final CompletableFuture<Boolean> queueFuture = this.blockingQueue.containsAsync(task).toCompletableFuture();
        return Optional.ofNullable(delayedFuture.join()).orElse(false) || Optional.ofNullable(queueFuture.join()).orElse(false);
    }

    @Override
    public void close() {
        this.isRunning = false;
    }

    @Nonnull
    @Override
    protected Logger log() {
        return log;
    }

    private void take(@Nonnull RedissonClient redissonClient) {
        while (this.isRunning && !redissonClient.isShuttingDown()) {
            try {
                this.executeTask(this.blockingQueue.take());
            } catch (InterruptedException e) {
                break;
            } catch (Exception ignored) {
            }
        }
    }
}
