package cn.maarlakes.common.queue.redis;

import cn.maarlakes.common.queue.AbstractBlockingQueue;
import jakarta.annotation.Nonnull;
import org.redisson.api.RBlockingQueue;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.function.Predicate;

/**
 * 基于 Redisson {@link RBlockingQueue} 的消息队列实现，支持跨进程消息传递。
 *
 * @author linjpxc
 */
class RedissonMessageQueue<T> extends AbstractBlockingQueue<T> {

    private final String name;
    private final RBlockingQueue<T> queue;

    public RedissonMessageQueue(@Nonnull String name, @Nonnull RBlockingQueue<T> queue, @Nonnull Executor executor) {
        super(executor, null);
        this.queue = queue;
        this.name = name;
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
    public T poll() {
        return this.queue.poll();
    }

    @Override
    public CompletionStage<T> pollAsync() {
        return this.queue.pollAsync().toCompletableFuture();
    }

    @Override
    public void clear() {
        this.queue.clear();
    }

    @Override
    public CompletionStage<Void> clearAsync() {
        return CompletableFuture.runAsync(this.queue::clear, this.executor);
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
    public List<? extends T> removeIf(@Nonnull Predicate<T> predicate) {
        final List<T> list = new ArrayList<>();
        this.queue.removeIf(item -> {
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
        return this.queue.contains(value);
    }

    @Override
    public CompletionStage<Boolean> containsAsync(@Nonnull T value) {
        return this.queue.containsAsync(value).toCompletableFuture();
    }
}
