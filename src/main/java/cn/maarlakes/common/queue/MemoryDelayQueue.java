package cn.maarlakes.common.queue;

import cn.maarlakes.common.utils.RateLimiter;
import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 基于 {@link java.util.concurrent.DelayQueue} 的内存延迟队列实现。
 *
 * <p>所有消息存储在 JVM 内存中，仅适用于单机场景。底层使用 {@link DelayQueue}，
 * 消息通过 {@link DelayedWrapper} 包装以支持延迟投递。
 *
 * <h3>延迟机制</h3>
 * <p>{@link #convert} 方法根据参数决定延迟策略：
 * <ul>
 *   <li>{@code delay == null} 且消息实现 {@link java.util.concurrent.Delayed} → 使用消息自带的延迟时间</li>
 *   <li>{@code delay == null} 且消息为普通对象 → 无延迟（{@link Duration#ZERO}），立即投递</li>
 *   <li>{@code delay != null} → 使用指定的延迟时间</li>
 * </ul>
 *
 * <h3>DelayedWrapper</h3>
 * <p>内部类 {@link DelayedWrapper} 将消息包装为 {@link java.util.concurrent.Delayed} 接口的实现。
 * 在构造时记录 {@link LocalDateTime#now()}，在 {@link DelayedWrapper#getDelay} 中计算剩余延迟时间。
 * {@link DelayedWrapper#equals} 和 {@link DelayedWrapper#hashCode} 委托给内部消息值，
 * 确保 {@link DelayQueue#remove} 等操作能按消息值匹配。
 *
 * @param <T> 消息类型
 * @author linjpxc
 */
public class MemoryDelayQueue<T> extends AbstractBlockingQueue<T> implements DelayedQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(MemoryDelayQueue.class);

    private final BlockingQueue<DelayedWrapper<T>> queue = new DelayQueue<>();
    private final String name;

    /**
     * 使用 {@link ForkJoinPool#commonPool()} 作为执行器。
     *
     * @param name         队列名称
     * @param rateLimiter  消息消费速率限制器，{@code null} 表示不限制
     */
    public MemoryDelayQueue(@Nonnull String name, RateLimiter rateLimiter) {
        this(name, ForkJoinPool.commonPool(), rateLimiter);
    }

    /**
     * @param name         队列名称
     * @param executor     用于异步执行监听器回调的执行器
     * @param rateLimiter  消息消费速率限制器，{@code null} 表示不限制
     */
    public MemoryDelayQueue(@Nonnull String name, @Nonnull Executor executor, RateLimiter rateLimiter) {
        super(executor, rateLimiter);
        this.name = name;
    }

    @Nonnull
    @Override
    public String name() {
        return this.name;
    }

    @Override
    public int size() {
        return this.queue.size();
    }

    @Override
    public CompletionStage<Integer> sizeAsync() {
        return CompletableFuture.completedFuture(this.queue.size());
    }

    @Override
    public boolean isEmpty() {
        return this.queue.isEmpty();
    }

    @Override
    public CompletionStage<Boolean> isEmptyAsync() {
        return CompletableFuture.completedFuture(this.queue.isEmpty());
    }

    /**
     * 投递消息到延迟队列。如果消息实现了 {@link java.util.concurrent.Delayed}，
     * 使用消息自带的延迟时间；否则无延迟，立即投递。
     */
    @Override
    public boolean offer(@Nonnull T value) {
        return this.queue.offer(convert(value, null));
    }

    @Override
    public CompletionStage<Boolean> offerAsync(@Nonnull T value) {
        return CompletableFuture.completedFuture(this.offer(value));
    }

    @Override
    public boolean offer(@Nonnull T value, @Nonnull Duration delay) {
        if (log.isDebugEnabled()) {
            log.debug("内存延迟队列 {} 投递消息，延迟: {}ms", this.name, delay.toMillis());
        }
        return this.queue.offer(this.convert(value, delay));
    }

    @Override
    public CompletionStage<Boolean> offerAsync(@Nonnull T value, @Nonnull Duration delay) {
        return CompletableFuture.completedFuture(this.offer(value, delay));
    }

    @Override
    public T poll() {
        final DelayedWrapper<T> wrapper = this.queue.poll();
        return wrapper == null ? null : wrapper.value;
    }

    @Override
    public CompletionStage<T> pollAsync() {
        return CompletableFuture.completedFuture(this.poll());
    }

    @Override
    public void clear() {
        this.queue.clear();
    }

    @Override
    public CompletionStage<Void> clearAsync() {
        this.queue.clear();
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public boolean remove(@Nonnull T value) {
        return this.queue.remove(convert(value, null));
    }

    @Override
    public CompletionStage<Boolean> removeAsync(@Nonnull T value) {
        return CompletableFuture.completedFuture(this.remove(value));
    }

    @Override
    public boolean removeAll(@Nonnull Collection<? extends T> values) {
        // 需要将消息值转换为 DelayedWrapper 以匹配队列中的元素
        final List<DelayedWrapper<T>> list = values.stream().map(item -> this.convert(item, null)).collect(Collectors.toList());
        return this.queue.removeAll(list);
    }

    @Override
    public CompletionStage<Boolean> removeAllAsync(@Nonnull Collection<? extends T> values) {
        return CompletableFuture.completedFuture(this.removeAll(values));
    }

    @Override
    public List<? extends T> removeIf(@Nonnull Predicate<T> predicate) {
        final List<T> list = new ArrayList<>();
        this.queue.removeIf(item -> {
            if (predicate.test(item.value)) {
                list.add(item.value);
                return true;
            }
            return false;
        });
        return list;
    }

    @Override
    public boolean contains(@Nonnull T value) {
        return this.queue.contains(this.convert(value, null));
    }

    @Override
    public CompletionStage<Boolean> containsAsync(@Nonnull T value) {
        return CompletableFuture.completedFuture(this.contains(value));
    }

    @Nonnull
    @Override
    protected T take() throws Exception {
        return this.queue.take().value;
    }

    /**
     * 将未确认的消息重新投递到队列，使用 {@link Duration#ZERO} 表示立即重新投递（无延迟）。
     */
    @Override
    protected void reOffer(@Nonnull T value) {
        if (log.isDebugEnabled()) {
            log.debug("内存延迟队列 {} 重新投递消息（无延迟）: {}", this.name, value);
        }
        this.queue.offer(this.convert(value, Duration.ZERO));
    }

    /**
     * 将消息转换为 {@link DelayedWrapper}。
     *
     * <p>三路分支逻辑：
     * <ol>
     *   <li>{@code delay == null} 且消息实现 {@link java.util.concurrent.Delayed} → 传入 {@code null} 延迟，
     *       {@link DelayedWrapper} 会委托给消息自身的 {@code getDelay} 方法</li>
     *   <li>{@code delay == null} 且消息为普通对象 → 使用 {@link Duration#ZERO}，立即投递</li>
     *   <li>{@code delay != null} → 使用指定的延迟时间</li>
     * </ol>
     */
    private DelayedWrapper<T> convert(@Nonnull T value, Duration delay) {
        if (delay == null) {
            if (value instanceof Delayed) {
                return new DelayedWrapper<>(value, null);
            }
            return new DelayedWrapper<>(value, Duration.ZERO);
        }
        return new DelayedWrapper<>(value, delay);
    }

    /**
     * 延迟消息包装器，将消息值包装为 {@link java.util.concurrent.Delayed} 接口的实现。
     *
     * <p>在构造时记录 {@link LocalDateTime#now()} 作为基准时间，
     * {@link #getDelay} 根据基准时间和指定延迟计算剩余等待时间。
     * 当消息本身实现了 {@link java.util.concurrent.Delayed} 且未指定延迟时间时，
     * 委托给消息自身的 {@code getDelay} 方法。
     *
     * <p>{@link #equals} 和 {@link #hashCode} 仅基于 {@link #value}，
     * 使得 {@link DelayQueue} 的 {@code remove}/{@code contains} 操作能按消息值匹配。
     */
    private static final class DelayedWrapper<T> implements Delayed {

        /** 基准时间，用于计算剩余延迟 */
        private final LocalDateTime now = LocalDateTime.now();
        /** 包装的消息值 */
        private final T value;
        /** 指定的延迟时间，{@code null} 表示委托给消息自身的延迟 */
        private final Duration delay;

        private DelayedWrapper(@Nonnull T value, Duration delay) {
            this.value = value;
            this.delay = delay;
        }

        /**
         * 计算剩余延迟时间。
         *
         * <p>如果 {@link #delay} 为 {@code null}（消息实现了 {@link java.util.concurrent.Delayed}），
         * 委托给消息自身的 {@code getDelay} 方法。否则根据基准时间和指定延迟计算剩余时间。
         * 剩余时间为负数时返回 0，表示消息已到期可被消费。
         */
        @Override
        public long getDelay(@Nonnull TimeUnit unit) {
            if (this.delay == null) {
                return ((Delayed) this.value).getDelay(unit);
            }
            final Duration elapsed = Duration.between(this.now, LocalDateTime.now());
            final Duration remaining = this.delay.minus(elapsed);
            if (remaining.isNegative()) {
                return 0;
            }
            return unit.convert(remaining.toNanos(), TimeUnit.NANOSECONDS);
        }

        /**
         * 比较两个延迟消息的到期时间。
         *
         * <p>如果消息本身实现了 {@link java.util.concurrent.Delayed}，委托给消息自身的 {@code compareTo}。
         * 否则按毫秒级延迟时间比较。
         */
        @Override
        public int compareTo(@Nonnull Delayed o) {
            if (this.value instanceof Delayed) {
                return ((Delayed) this.value).compareTo(o);
            }
            final long diff = this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
            return Long.compare(diff, 0);
        }

        /**
         * 基于消息值判断相等性，用于 {@link DelayQueue#remove} 和 {@link DelayQueue#contains} 操作。
         */
        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o instanceof DelayedWrapper) {
                final DelayedWrapper<?> that = (DelayedWrapper<?>) o;
                return Objects.equals(this.value, that.value);
            }
            return false;
        }

        /**
         * 基于消息值计算哈希，与 {@link #equals} 保持一致。
         */
        @Override
        public int hashCode() {
            return Objects.hash(this.value);
        }
    }
}
