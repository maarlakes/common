package cn.maarlakes.common.queue.redis;

import cn.maarlakes.common.queue.AbstractBlockingQueue;
import cn.maarlakes.common.queue.DelayedQueue;
import cn.maarlakes.common.utils.RateLimiter;
import jakarta.annotation.Nonnull;
import org.redisson.api.RBlockingQueue;
import org.redisson.api.RDelayedQueue;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * @author linjpxc
 */
class RedissonDelayQueue<T> extends AbstractBlockingQueue<T> implements DelayedQueue<T> {
    private final String name;
    private final RBlockingQueue<T> queue;
    private final RDelayedQueue<T> delayedQueue;

    protected RedissonDelayQueue(@Nonnull String name, @Nonnull RBlockingQueue<T> queue, RDelayedQueue<T> delayedQueue, @Nonnull Executor executor, RateLimiter rateLimiter) {
        super(executor, rateLimiter);
        this.name = name;
        this.queue = queue;
        this.delayedQueue = delayedQueue;
    }

    @Override
    public boolean offer(@Nonnull T value) {
        if (value instanceof Delayed) {
            final Delayed delayed = (Delayed) value;
            this.delayedQueue.offer(value, delayed.getDelay(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS);
        } else {
            return this.queue.offer(value);
        }
        return true;
    }

    @Override
    public CompletionStage<Boolean> offerAsync(@Nonnull T value) {
        if (value instanceof Delayed) {
            final Delayed delayed = (Delayed) value;
            return this.delayedQueue.offerAsync(value, delayed.getDelay(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS).thenApply(v -> true);
        }
        return this.queue.offerAsync(value);
    }

    @Override
    public boolean offer(@Nonnull T value, @Nonnull Duration delay) {
        this.delayedQueue.offer(value, delay.toMillis(), TimeUnit.MILLISECONDS);
        return true;
    }

    @Override
    public CompletionStage<Boolean> offerAsync(@Nonnull T value, @Nonnull Duration delay) {
        return this.delayedQueue.offerAsync(value, delay.toMillis(), TimeUnit.MILLISECONDS).thenApply(v -> true);
    }

    @Nonnull
    @Override
    public String name() {
        return this.name;
    }

    @Override
    public int size() {
        return this.queue.size() + this.delayedQueue.size();
    }

    @Override
    public CompletionStage<Integer> sizeAsync() {
        return this.queue.sizeAsync().thenCombine(this.delayedQueue.sizeAsync(), Integer::sum);
    }

    @Override
    public boolean isEmpty() {
        return this.queue.isEmpty() && this.delayedQueue.isEmpty();
    }

    @Override
    public CompletionStage<Boolean> isEmptyAsync() {
        return CompletableFuture.supplyAsync(this::isEmpty);
    }

    @Override
    public void clear() {
        this.delayedQueue.clear();
        this.queue.clear();
    }

    @Override
    public CompletionStage<Void> clearAsync() {
        return CompletableFuture.runAsync(this::clear);
    }

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

    @Override
    public boolean contains(@Nonnull T value) {
        return this.delayedQueue.contains(value) || this.queue.contains(value);
    }

    @Override
    public CompletionStage<Boolean> containsAsync(@Nonnull T value) {
        return this.delayedQueue.containsAsync(value).thenCombine(this.queue.containsAsync(value), (a, b) -> a || b);
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        final Iterator<T> iterator = this.delayedQueue.iterator();
        final Iterator<T> iterator1 = this.queue.iterator();
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext() || iterator1.hasNext();
            }

            @Override
            public T next() {
                if (iterator.hasNext()) {
                    return iterator.next();
                }
                return iterator1.next();
            }
        };
    }

    @Nonnull
    @Override
    protected T take() throws Exception {
        return this.queue.take();
    }
}
