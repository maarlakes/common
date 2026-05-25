package cn.maarlakes.common.queue.redis;

import cn.maarlakes.common.queue.AbstractBlockingQueue;
import cn.maarlakes.common.queue.DelayedQueue;
import cn.maarlakes.common.utils.RateLimiter;
import jakarta.annotation.Nonnull;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * 基于 Redisson {@link RDelayedQueue} + {@link RBlockingQueue} 的延迟队列实现。
 *
 * <p>消息通过 {@link RDelayedQueue} 投递，到期后自动转移到 {@link RBlockingQueue} 供消费。
 * 支持跨进程消息传递，消息存储在 Redis 中。
 *
 * <h3>双队列架构</h3>
 * <p>消息生命周期：投递到 {@link RDelayedQueue} → 等待延迟到期 → 自动转移到 {@link RBlockingQueue} → 消费者 {@code take()} 获取。
 *
 * <h3>延迟检测</h3>
 * <p>调用 {@link #offer(Object)} 时，如果消息实现了 {@link java.util.concurrent.Delayed} 接口，
 * 会自动使用其 {@code getDelay} 返回的延迟时间。否则无延迟，消息直接进入 {@link RBlockingQueue}。
 *
 * <p>关闭时需要调用 {@link RDelayedQueue#destroy()} 释放 Redisson 内部的转移调度资源。
 *
 * @param <T> 消息类型
 * @see cn.maarlakes.common.queue.MemoryDelayQueue
 * @author linjpxc
 */
class RedissonDelayQueue<T> extends AbstractBlockingQueue<T> implements DelayedQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(RedissonDelayQueue.class);

    private final String name;
    /** 就绪队列，存放延迟已到期的消息，消费者从此队列获取 */
    private final RBlockingQueue<T> queue;
    /** 延迟队列，消息先进入此队列，到期后自动转移到 {@link #queue} */
    private final RDelayedQueue<T> delayedQueue;
    /** 保证 delayedQueue.destroy() 只执行一次 */
    private final AtomicBoolean destroyed = new AtomicBoolean();

    /**
     * @param name          队列名称
     * @param queue         就绪阻塞队列（延迟到期后的消息存放于此）
     * @param delayedQueue  延迟队列（消息先进入此队列等待到期）
     * @param executor      用于异步执行监听器回调的执行器
     * @param rateLimiter   消息消费速率限制器，{@code null} 表示不限制
     */
    protected RedissonDelayQueue(@Nonnull String name, @Nonnull RBlockingQueue<T> queue,
                                 RDelayedQueue<T> delayedQueue, @Nonnull Executor executor,
                                 RateLimiter rateLimiter) {
        super(executor, rateLimiter);
        this.name = name;
        this.queue = queue;
        this.delayedQueue = delayedQueue;
    }

    /**
     * 投递消息到延迟队列。
     *
     * <p>如果消息实现了 {@link java.util.concurrent.Delayed}，使用其自带的延迟时间；
     * 否则以 0ms 延迟投递，消息立即进入就绪队列。
     */
    @Override
    public boolean offer(@Nonnull T value) {
        if (value instanceof Delayed) {
            // 消息自带延迟时间，委托给 Delayed 接口
            final Delayed delayed = (Delayed) value;
            this.delayedQueue.offer(value, delayed.getDelay(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        } else {
            // 普通消息，无延迟直接进入就绪队列
            this.delayedQueue.offer(value, 0, TimeUnit.MILLISECONDS);
        }
        if (log.isDebugEnabled()) {
            log.debug("Redisson 延迟队列 {} 投递消息", this.name);
        }
        return true;
    }

    /**
     * 异步投递消息。委托给 Redisson 的异步 API。
     *
     * <p>使用 {@code thenApply(v -> true)} 将 Redisson 返回的 {@code Void} 转换为 {@code Boolean}。
     */
    @Override
    public CompletionStage<Boolean> offerAsync(@Nonnull T value) {
        if (value instanceof Delayed) {
            final Delayed delayed = (Delayed) value;
            return this.delayedQueue.offerAsync(value, delayed.getDelay(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS).thenApply(v -> true);
        }
        return this.delayedQueue.offerAsync(value, 0, TimeUnit.MILLISECONDS).thenApply(v -> true);
    }

    @Override
    public boolean offer(@Nonnull T value, @Nonnull Duration delay) {
        if (log.isDebugEnabled()) {
            log.debug("Redisson 延迟队列 {} 投递消息，延迟: {}ms", this.name, delay.toMillis());
        }
        this.delayedQueue.offer(value, delay.toMillis(), TimeUnit.MILLISECONDS);
        return true;
    }

    /**
     * 异步按延迟投递消息。委托给 Redisson 的异步 API。
     */
    @Override
    public CompletionStage<Boolean> offerAsync(@Nonnull T value, @Nonnull Duration delay) {
        return this.delayedQueue.offerAsync(value, delay.toMillis(), TimeUnit.MILLISECONDS).thenApply(v -> true);
    }

    @Nonnull
    @Override
    public String name() {
        return this.name;
    }

    /**
     * 队列中的消息总数，包括延迟队列中等待到期的消息和就绪队列中可消费的消息。
     */
    @Override
    public int size() {
        // 延迟队列 + 就绪队列的消息数量之和
        return this.queue.size() + this.delayedQueue.size();
    }

    @Override
    public CompletionStage<Integer> sizeAsync() {
        return this.queue.sizeAsync().thenCombine(this.delayedQueue.sizeAsync(), Integer::sum);
    }

    /**
     * 两个队列都为空时才返回 {@code true}。
     */
    @Override
    public boolean isEmpty() {
        return this.queue.isEmpty() && this.delayedQueue.isEmpty();
    }

    @Override
    public CompletionStage<Boolean> isEmptyAsync() {
        return CompletableFuture.supplyAsync(this::isEmpty, this.executor);
    }

    @Override
    public T poll() {
        return this.queue.poll();
    }

    @Override
    public CompletionStage<T> pollAsync() {
        return this.queue.pollAsync().toCompletableFuture();
    }

    @Override
    public void clear() {
        if (log.isDebugEnabled()) {
            log.debug("Redisson 延迟队列 {} 清空", this.name);
        }
        this.delayedQueue.clear();
        this.queue.clear();
    }

    @Override
    public CompletionStage<Void> clearAsync() {
        return CompletableFuture.runAsync(this::clear, this.executor);
    }

    /**
     * 从延迟队列和就绪队列中移除指定消息。只要在任一队列中找到即返回 {@code true}。
     */
    @Override
    public boolean remove(@Nonnull T value) {
        final boolean a = this.delayedQueue.remove(value);
        final boolean b = this.queue.remove(value);
        return a || b;
    }

    @Override
    public CompletionStage<Boolean> removeAsync(@Nonnull T value) {
        return this.delayedQueue.removeAsync(value).thenCombine(this.queue.removeAsync(value), (a, b) -> a || b);
    }

    @Override
    public boolean removeAll(@Nonnull Collection<? extends T> values) {
        final boolean a = this.delayedQueue.removeAll(values);
        final boolean b = this.queue.removeAll(values);
        return a || b;
    }

    @Override
    public CompletionStage<Boolean> removeAllAsync(@Nonnull Collection<? extends T> values) {
        return this.delayedQueue.removeAllAsync(values).thenCombine(this.queue.removeAllAsync(values), (a, b) -> a || b);
    }

    /**
     * 按条件从延迟队列和就绪队列中移除消息。两个队列中的匹配消息都会被移除。
     */
    @Override
    public List<? extends T> removeIf(@Nonnull Predicate<T> predicate) {
        final List<T> list = new ArrayList<>();
        this.queue.removeIf(item -> {
            if (predicate.test(item)) {
                list.add(item);
                return true;
            }
            return false;
        });
        this.delayedQueue.removeIf(item -> {
            if (predicate.test(item)) {
                list.add(item);
                return true;
            }
            return false;
        });
        return list;
    }

    /**
     * 检查延迟队列或就绪队列中是否包含指定消息。
     */
    @Override
    public boolean contains(@Nonnull T value) {
        return this.delayedQueue.contains(value) || this.queue.contains(value);
    }

    @Override
    public CompletionStage<Boolean> containsAsync(@Nonnull T value) {
        return this.delayedQueue.containsAsync(value).thenCombine(this.queue.containsAsync(value), (a, b) -> a || b);
    }

    /**
     * 从就绪队列阻塞获取一条消息，对应 Redis 的 BLPOP 命令。
     * 延迟队列中的消息到期后会自动转移到就绪队列。
     */
    @Nonnull
    @Override
    protected T take() throws Exception {
        return this.queue.take();
    }

    /**
     * 将未确认的消息重新投递到延迟队列，使用 0ms 延迟表示立即重新投递。
     */
    @Override
    protected void reOffer(@Nonnull T value) {
        if (log.isDebugEnabled()) {
            log.debug("Redisson 延迟队列 {} 重新投递消息（无延迟）: {}", this.name, value);
        }
        this.delayedQueue.offer(value, 0, TimeUnit.MILLISECONDS);
    }

    /**
     * 关闭队列。除了停止消费线程外，还需要调用 {@link RDelayedQueue#destroy()}
     * 释放 Redisson 内部的转移调度资源（定时任务、Redis 订阅等）。
     */
    @Override
    public void close() {
        super.close();
        if (this.destroyed.compareAndSet(false, true)) {
            if (log.isDebugEnabled()) {
                log.debug("Redisson 延迟队列 {} 关闭，销毁延迟队列资源", this.name);
            }
            this.delayedQueue.destroy();
        }
    }
}
