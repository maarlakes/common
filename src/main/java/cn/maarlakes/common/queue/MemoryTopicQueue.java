package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.*;

/**
 * @author linjpxc
 */
public class MemoryTopicQueue<T> extends AbstractBlockingQueue<T> {

    private final BlockingQueue<T> queue;

    private final String name;

    public MemoryTopicQueue(@Nonnull String name) {
        this(name, new LinkedBlockingQueue<>(), new ForkJoinPool());
    }

    public MemoryTopicQueue(@Nonnull String name, @Nonnull Executor executor) {
        this(name, new LinkedBlockingQueue<>(), executor);
    }

    public MemoryTopicQueue(@Nonnull String name, BlockingQueue<T> queue) {
        this(name, queue, new ForkJoinPool());
    }

    public MemoryTopicQueue(@Nonnull String name, BlockingQueue<T> queue, @Nonnull Executor executor) {
        super(executor);
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
    public boolean contains(@Nonnull T value) {
        return this.queue.contains(value);
    }

    @Override
    public CompletionStage<Boolean> containsAsync(@Nonnull T value) {
        return CompletableFuture.completedFuture(this.queue.contains(value));
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return this.queue.iterator();
    }

    @Nonnull
    @Override
    protected T take() throws Exception {
        return this.queue.take();
    }
}
