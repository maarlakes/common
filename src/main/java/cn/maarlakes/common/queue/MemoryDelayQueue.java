package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

/**
 * @author linjpxc
 */
public class MemoryDelayQueue<T> extends AbstractBlockingQueue<T> implements DelayedQueue<T> {

    private final BlockingQueue<DelayedWrapper<T>> queue = new DelayQueue<>();
    private final String name;

    public MemoryDelayQueue(@Nonnull String name) {
        this(name, new ForkJoinPool());
    }

    public MemoryDelayQueue(@Nonnull String name, @Nonnull Executor executor) {
        super(executor);
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
    public boolean contains(@Nonnull T value) {
        return this.queue.contains(this.convert(value, null));
    }

    @Override
    public CompletionStage<Boolean> containsAsync(@Nonnull T value) {
        return CompletableFuture.completedFuture(this.contains(value));
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        final Iterator<DelayedWrapper<T>> iterator = this.queue.iterator();
        return new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return iterator.hasNext();
            }

            @Override
            public T next() {
                return iterator.next().value;
            }
        };
    }

    @Nonnull
    @Override
    protected T take() throws Exception {
        return this.queue.take().value;
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

        public final LocalDateTime now = LocalDateTime.now();
        public final T value;
        public final Duration delay;

        private DelayedWrapper(@Nonnull T value, Duration delay) {
            this.value = value;
            this.delay = delay;
        }

        @Override
        public long getDelay(@Nonnull TimeUnit unit) {
            if (this.delay == null) {
                return ((Delayed) this.value).getDelay(unit);
            }
            final Duration duration = Duration.between(this.now, LocalDateTime.now());
            return unit.convert(this.delay.minus(duration).toMillis(), TimeUnit.MILLISECONDS);
        }

        @Override
        public int compareTo(@Nonnull Delayed o) {
            if (this.value instanceof Delayed) {
                return ((Delayed) this.value).compareTo(o);
            }
            return 0;
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
