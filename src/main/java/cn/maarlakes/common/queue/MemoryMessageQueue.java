package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * 基于 {@link java.util.concurrent.BlockingQueue} 的内存消息队列实现。
 *
 * @author linjpxc
 */
public class MemoryMessageQueue<T> extends AbstractBlockingQueue<T> {

    private final BlockingQueue<T> queue;
    private final String name;

    public MemoryMessageQueue(@Nonnull String name) {
        this(name, new LinkedBlockingQueue<>(), ForkJoinPool.commonPool());
    }

    public MemoryMessageQueue(@Nonnull String name, @Nonnull Executor executor) {
        this(name, new LinkedBlockingQueue<>(), executor);
    }

    public MemoryMessageQueue(@Nonnull String name, BlockingQueue<T> queue) {
        this(name, queue, ForkJoinPool.commonPool());
    }

    public MemoryMessageQueue(@Nonnull String name, BlockingQueue<T> queue, @Nonnull Executor executor) {
        super(executor, null);
        this.name = name;
        this.queue = queue;
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
        return this.queue.offer(value);
    }

    @Override
    public CompletionStage<Boolean> offerAsync(@Nonnull T value) {
        return CompletableFuture.completedFuture(this.offer(value));
    }

    @Override
    public T poll() {
        return this.queue.poll();
    }

    @Override
    public CompletionStage<T> pollAsync() {
        return CompletableFuture.completedFuture(this.queue.poll());
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
        return this.queue.remove(value);
    }

    @Override
    public CompletionStage<Boolean> removeAsync(@Nonnull T value) {
        return CompletableFuture.completedFuture(this.remove(value));
    }

    @Override
    public boolean removeAll(@Nonnull Collection<? extends T> values) {
        return this.queue.removeAll(values);
    }

    @Override
    public CompletionStage<Boolean> removeAllAsync(@Nonnull Collection<? extends T> values) {
        return CompletableFuture.completedFuture(this.removeAll(values));
    }

    @Override
    public List<? extends T> removeIf(@Nonnull Predicate<T> predicate) {
        final List<T> list = new ArrayList<>();
        this.queue.removeIf(item -> {
            final boolean result = predicate.test(item);
            if (result) {
                list.add(item);
            }
            return result;
        });
        return list;
    }

    @Override
    public boolean contains(@Nonnull T value) {
        return this.queue.contains(value);
    }

    @Override
    public CompletionStage<Boolean> containsAsync(@Nonnull T value) {
        return CompletableFuture.completedFuture(this.queue.contains(value));
    }

    @Nonnull
    @Override
    protected T take() throws Exception {
        return this.queue.take();
    }

    @Override
    protected void reOffer(@Nonnull T value) {
        this.queue.offer(value);
    }
}
