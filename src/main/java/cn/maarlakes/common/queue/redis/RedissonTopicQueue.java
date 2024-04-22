package cn.maarlakes.common.queue.redis;

import cn.maarlakes.common.queue.AbstractBlockingQueue;
import jakarta.annotation.Nonnull;
import org.redisson.api.RBlockingQueue;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
class RedissonTopicQueue<T> extends AbstractBlockingQueue<T> {

    private final String name;
    private final RBlockingQueue<T> queue;

    public RedissonTopicQueue(@Nonnull String name, @Nonnull RBlockingQueue<T> queue, @Nonnull Executor executor) {
        super(executor);
        this.queue = queue;
        this.name = name;
    }

    @Nonnull
    @Override
    protected T take() throws Exception {
        return this.queue.take();
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
        return this.queue.sizeAsync().toCompletableFuture();
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
        return this.queue.offerAsync(value).toCompletableFuture();
    }

    @Override
    public void clear() {
        this.queue.clear();
    }

    @Override
    public CompletionStage<Void> clearAsync() {
        return CompletableFuture.runAsync(this.queue::clear);
    }

    @Override
    public boolean remove(@Nonnull T value) {
        return this.queue.remove(value);
    }

    @Override
    public CompletionStage<Boolean> removeAsync(@Nonnull T value) {
        return this.queue.removeAsync(value).toCompletableFuture();
    }

    @Override
    public boolean removeAll(@Nonnull Collection<? extends T> values) {
        return this.queue.removeAll(values);
    }

    @Override
    public CompletionStage<Boolean> removeAllAsync(@Nonnull Collection<? extends T> values) {
        return this.queue.removeAllAsync(values).toCompletableFuture();
    }

    @Override
    public boolean contains(@Nonnull T value) {
        return this.queue.contains(value);
    }

    @Override
    public CompletionStage<Boolean> containsAsync(@Nonnull T value) {
        return this.queue.containsAsync(value).toCompletableFuture();
    }

    @Nonnull
    @Override
    public Iterator<T> iterator() {
        return this.queue.iterator();
    }
}
