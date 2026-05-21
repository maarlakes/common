package cn.maarlakes.common.queue;

import cn.maarlakes.common.utils.RateLimiter;
import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 基于 {@link java.util.concurrent.DelayQueue} 的内存延迟队列实现。
 *
 * <p>消息通过 {@link DelayedWrapper} 包装以支持延迟投递。
 * 如果消息本身实现了 {@link java.util.concurrent.Delayed}，会自动使用其延迟时间。
 *
 * @author linjpxc
 */
public class MemoryDelayQueue<T> extends AbstractBlockingQueue<T> implements DelayedQueue<T> {

    private final BlockingQueue<DelayedWrapper<T>> queue = new DelayQueue<>();
    private final String name;

    public MemoryDelayQueue(@Nonnull String name, RateLimiter rateLimiter) {
        this(name, ForkJoinPool.commonPool(), rateLimiter);
    }

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

    @Override
    protected void reOffer(@Nonnull T value) {
        this.queue.offer(this.convert(value, Duration.ZERO));
    }

    private DelayedWrapper<T> convert(@Nonnull T value, Duration delay) {
        if (delay == null) {
            if (value instanceof Delayed) {
                return new DelayedWrapper<>(value, null);
            }
            return new DelayedWrapper<>(value, Duration.ZERO);
        }
        return new DelayedWrapper<>(value, delay);
    }

    private static final class DelayedWrapper<T> implements Delayed {

        private final LocalDateTime now = LocalDateTime.now();
        private final T value;
        private final Duration delay;

        private DelayedWrapper(@Nonnull T value, Duration delay) {
            this.value = value;
            this.delay = delay;
        }

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

        @Override
        public int compareTo(@Nonnull Delayed o) {
            if (this.value instanceof Delayed) {
                return ((Delayed) this.value).compareTo(o);
            }
            final long diff = this.getDelay(TimeUnit.MILLISECONDS) - o.getDelay(TimeUnit.MILLISECONDS);
            return Long.compare(diff, 0);
        }

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

        @Override
        public int hashCode() {
            return Objects.hash(this.value);
        }
    }
}
