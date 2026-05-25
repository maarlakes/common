package cn.maarlakes.common.task.redisson;

import cn.maarlakes.common.task.AbstractScheduledTaskService;
import cn.maarlakes.common.task.TaskExecutor;
import jakarta.annotation.Nonnull;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
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
 * 基于 Redisson 延迟队列的分布式调度任务服务。
 *
 * <p>利用 Redis 的 {@link RDelayedQueue} 和 {@link RBlockingQueue} 实现跨进程的延迟调度。
 * 架构如下：</p>
 * <ol>
 *     <li>任务通过 {@link #scheduleAsync} 投入 {@link RDelayedQueue}，携带指定的延迟时间</li>
 *     <li>延迟到期后，Redisson 自动将任务从延迟队列转移到 {@link RBlockingQueue}</li>
 *     <li>后台守护线程通过 {@link RBlockingQueue#take()} 阻塞消费，取出任务后执行</li>
 * </ol>
 *
 * <p>数据序列化使用 {@link Kryo5Codec}，队列命名规则为 {@code namespace:taskName}。</p>
 *
 * <p>适用于多实例部署环境，确保同一任务在集群中只被一个实例执行。
 * 单实例场景可使用 {@link cn.maarlakes.common.task.SystemScheduledTaskService} 以避免 Redis 依赖。</p>
 *
 * @param <T> 任务数据的类型，需可序列化
 * @author linjpxc
 */
public class RedissonScheduledTaskService<T> extends AbstractScheduledTaskService<T> implements Closeable {
    private static final Logger log = LoggerFactory.getLogger(RedissonScheduledTaskService.class);

    /** Redis 延迟队列，任务在此等待延迟到期 */
    private final RDelayedQueue<T> delayedQueue;

    /** Redis 阻塞队列，延迟到期后任务转移至此，守护线程从此消费 */
    private final RBlockingQueue<T> blockingQueue;

    /** 任务名称标识 */
    private final String taskName;

    /** 后台守护线程，负责从阻塞队列中消费任务并执行 */
    private final Thread daemonThread;

    /** 控制守护线程生命周期的运行标志 */
    private volatile boolean isRunning = true;

    /**
     * 创建 Redisson 调度任务服务，使用 {@link java.util.concurrent.ForkJoinPool#commonPool()} 作为执行线程池。
     *
     * @param redissonClient Redisson 客户端
     * @param namespace      Redis key 命名空间前缀
     * @param taskName       任务名称
     * @param taskExecutors  任务执行器列表
     */
    public RedissonScheduledTaskService(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull String taskName, @Nonnull List<? extends TaskExecutor<T>> taskExecutors) {
        this(redissonClient, namespace, taskName, taskExecutors, ForkJoinPool.commonPool());
    }

    /**
     * 创建 Redisson 调度任务服务。
     *
     * <p>创建流程：</p>
     * <ol>
     *     <li>创建 {@link RBlockingQueue}，队列 key 为 {@code namespace:taskName}</li>
     *     <li>创建绑定到阻塞队列的 {@link RDelayedQueue}</li>
     *     <li>启动守护线程，循环消费阻塞队列中的任务</li>
     * </ol>
     *
     * @param redissonClient Redisson 客户端
     * @param namespace      Redis key 命名空间前缀
     * @param taskName       任务名称
     * @param taskExecutors  任务执行器列表
     * @param executor       异步执行线程池
     */
    public RedissonScheduledTaskService(@Nonnull RedissonClient redissonClient, @Nonnull String namespace, @Nonnull String taskName, @Nonnull List<? extends TaskExecutor<T>> taskExecutors, @Nonnull Executor executor) {
        super(taskExecutors, executor);
        final Kryo5Codec codec = new Kryo5Codec();
        final String queueName = namespace.endsWith(":") ? namespace + taskName : namespace + ":" + taskName;
        this.blockingQueue = redissonClient.getBlockingQueue(queueName, codec);
        this.delayedQueue = redissonClient.getDelayedQueue(this.blockingQueue);

        this.taskName = taskName;
        log.info("创建 Redisson 调度任务服务，任务名称：{}，队列：{}", taskName, queueName);

        final Thread executeThread = new Thread(() -> this.take(redissonClient), "redisson-schedule-task-daemon-" + taskName);
        executeThread.setDaemon(true);
        this.daemonThread = executeThread;
        executeThread.start();
    }

    @Nonnull
    @Override
    public String getTaskName() {
        return this.taskName;
    }

    /**
     * 获取当前待执行的任务数量（延迟队列 + 就绪队列之和）。
     *
     * <p>两个队列的大小查询均容忍异常（返回 0），避免 Redis 连接问题导致方法失败。</p>
     *
     * @return 待执行任务数量
     */
    @Override
    public int taskCount() {
        final CompletableFuture<Integer> delayedFuture = this.delayedQueue.sizeAsync().toCompletableFuture().exceptionally(ex -> 0);
        final CompletableFuture<Integer> queueFuture = this.blockingQueue.sizeAsync().toCompletableFuture().exceptionally(ex -> 0);
        CompletableFuture.allOf(delayedFuture, queueFuture).join();
        return Optional.ofNullable(delayedFuture.join()).orElse(0)
                + Optional.ofNullable(queueFuture.join()).orElse(0);
    }

    /**
     * 异步调度任务。
     *
     * <p>先从延迟队列和阻塞队列中移除同任务（去重），然后将任务投入延迟队列。</p>
     *
     * @param task  任务数据
     * @param delay 延迟时间
     * @return 表示调度完成的 {@code CompletionStage}
     */
    @Override
    public CompletionStage<Void> scheduleAsync(@Nonnull T task, @Nonnull Duration delay) {
        if (log.isDebugEnabled()) {
            log.debug("调度任务 [{}] 延迟 {}ms，任务内容：{}", this.taskName, delay.toMillis(), task);
        }
        return CompletableFuture.allOf(
                this.delayedQueue.removeAsync(task).toCompletableFuture(),
                this.blockingQueue.removeAsync(task).toCompletableFuture()
        ).thenCompose(v -> this.delayedQueue.offerAsync(task, delay.toMillis(), TimeUnit.MILLISECONDS).toCompletableFuture());
    }

    /**
     * 清除延迟队列和就绪队列中的所有任务。
     */
    @Override
    public void clear() {
        log.info("清除任务 [{}] 的所有待执行调度", this.taskName);
        this.delayedQueue.clear();
        this.blockingQueue.clear();
    }

    /**
     * 从延迟队列和就绪队列中移除指定任务。
     *
     * <p>两个队列的移除操作均容忍异常（返回 {@code false}）。</p>
     *
     * @param task 要取消的任务
     */
    @Override
    public void cancelTask(@Nonnull T task) {
        if (log.isDebugEnabled()) {
            log.debug("取消任务 [{}] 的调度，任务内容：{}", this.taskName, task);
        }
        CompletableFuture.allOf(
                this.delayedQueue.removeAsync(task).toCompletableFuture().exceptionally(ex -> false),
                this.blockingQueue.removeAsync(task).toCompletableFuture().exceptionally(ex -> false)
        ).join();
    }

    /**
     * 检查指定任务是否在延迟队列或就绪队列中。
     *
     * @param task 要检查的任务
     * @return 如果任务在任一队列中则返回 {@code true}
     */
    @Override
    public boolean containsTask(@Nonnull T task) {
        final CompletableFuture<Boolean> delayedFuture = this.delayedQueue.containsAsync(task).toCompletableFuture().exceptionally(ex -> false);
        final CompletableFuture<Boolean> queueFuture = this.blockingQueue.containsAsync(task).toCompletableFuture().exceptionally(ex -> false);
        CompletableFuture.allOf(delayedFuture, queueFuture).join();
        return Optional.ofNullable(delayedFuture.join()).orElse(false) || Optional.ofNullable(queueFuture.join()).orElse(false);
    }

    /**
     * 关闭服务，停止守护线程。
     *
     * <p>设置运行标志为 {@code false} 并中断守护线程。
     * 不会清除队列中已有的任务。</p>
     */
    @Override
    public void close() {
        log.info("关闭 Redisson 调度任务服务，任务名称：{}", this.taskName);
        this.isRunning = false;
        this.daemonThread.interrupt();
    }

    @Nonnull
    @Override
    protected Logger log() {
        return log;
    }

    /**
     * 守护线程的主循环，持续从阻塞队列中消费任务并执行。
     *
     * <p>循环条件：服务仍在运行且 Redisson 客户端未关闭。
     * 当线程被中断时（{@link InterruptedException}）优雅退出；
     * 其他异常会记录日志后继续循环。</p>
     *
     * @param redissonClient Redisson 客户端，用于检测关闭状态
     */
    private void take(@Nonnull RedissonClient redissonClient) {
        while (this.isRunning && !redissonClient.isShuttingDown()) {
            try {
                final T task = this.blockingQueue.take();
                if (log.isDebugEnabled()) {
                    log.debug("调度任务 [{}] 从队列取出任务，开始执行，任务内容：{}", this.taskName, task);
                }
                this.executeTask(task);
            } catch (InterruptedException e) {
                log.warn("调度任务 [{}] 守护线程被中断，退出消费循环", this.taskName);
                break;
            } catch (Exception e) {
                if (this.isRunning) {
                    log.error("调度任务 [{}] take 异常", this.taskName, e);
                }
            }
        }
    }
}
