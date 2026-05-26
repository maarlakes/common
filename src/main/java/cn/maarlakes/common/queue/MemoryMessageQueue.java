package cn.maarlakes.common.queue;

import jakarta.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Predicate;

/**
 * 基于 {@link java.util.concurrent.BlockingQueue} 的内存消息队列实现。
 *
 * <p>所有消息存储在 JVM 内存中，仅适用于单机场景，不支持跨进程消息传递。
 * 底层使用 {@link BlockingQueue}，天然线程安全。
 *
 * <p>默认使用 {@link LinkedBlockingQueue}（无界队列）和 {@link ForkJoinPool#commonPool()} 作为执行器。
 *
 * @param <T> 消息类型
 * @author linjpxc
 */
public class MemoryMessageQueue<T> extends AbstractBlockingQueue<T> {

    private static final Logger log = LoggerFactory.getLogger(MemoryMessageQueue.class);

    private final BlockingQueue<T> queue;
    private final String name;

    /**
     * 使用默认配置创建内存消息队列。
     * 底层使用 {@link LinkedBlockingQueue}（无界），执行器使用 {@link ForkJoinPool#commonPool()}。
     *
     * @param name 队列名称
     */
    public MemoryMessageQueue(@Nonnull String name) {
        this(name, new LinkedBlockingQueue<>(), ForkJoinPool.commonPool());
    }

    /**
     * 使用自定义执行器创建内存消息队列。
     *
     * @param name     队列名称
     * @param executor 用于异步执行监听器回调的执行器
     */
    public MemoryMessageQueue(@Nonnull String name, @Nonnull Executor executor) {
        this(name, new LinkedBlockingQueue<>(), executor);
    }

    /**
     * 使用自定义阻塞队列创建内存消息队列。
     *
     * @param name  队列名称
     * @param queue 底层阻塞队列实现
     */
    public MemoryMessageQueue(@Nonnull String name, BlockingQueue<T> queue) {
        this(name, queue, ForkJoinPool.commonPool());
    }

    /**
     * 完整参数构造函数。
     *
     * @param name     队列名称
     * @param queue    底层阻塞队列实现
     * @param executor 用于异步执行监听器回调的执行器
     */
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
        // 同时执行判断和收集：满足条件的消息从队列移除并加入结果列表
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

    /**
     * 阻塞获取一条消息。由消费线程循环调用，队列无消息时阻塞等待。
     *
     * @return 获取到的消息
     */
    @Nonnull
    @Override
    protected T take() throws Exception {
        return this.queue.take();
    }

    /**
     * 将未确认的消息重新投递到队列。使用非阻塞的 {@link BlockingQueue#offer}，
     * 对于无界队列（如默认的 {@link LinkedBlockingQueue}）始终成功。
     */
    @Override
    protected void reOffer(@Nonnull T value) {
        if (log.isDebugEnabled()) {
            log.debug("内存队列 {} 重新投递消息: {}", this.name, value);
        }
        this.queue.offer(value);
    }
}
