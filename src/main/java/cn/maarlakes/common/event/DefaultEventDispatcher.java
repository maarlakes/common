package cn.maarlakes.common.event;

import cn.maarlakes.common.utils.ExecutorFactory;
import cn.maarlakes.common.utils.Lazy;
import cn.maarlakes.common.utils.SharedExecutorFactory;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * 默认的事件分发器实现，提供同步和异步两种分发模式。
 *
 * <p>同步分发直接在调用线程中执行监听器方法；异步分发通过内部线程池提交任务。
 * 线程池使用 {@link Lazy} 延迟初始化，首次异步分发时才创建。
 *
 * @author linjpxc
 */
public class DefaultEventDispatcher implements EventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventDispatcher.class);

    /** 延迟初始化的执行器 */
    @Nonnull
    private final Lazy<Executor> executor;

    /**
     * 创建固定线程数的分发器。
     *
     * <p>内部创建一个固定大小的线程池，队列容量 1000，饱和策略为 {@link ThreadPoolExecutor.CallerRunsPolicy}。
     *
     * @param threads 线程池大小
     */
    public DefaultEventDispatcher(int threads) {
        this(new ThreadPoolExecutor(threads, threads, 1L, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy()));
    }

    /**
     * 使用指定的 {@link ExecutorService} 创建分发器。
     *
     * @param executor 线程池，不能为 null
     */
    public DefaultEventDispatcher(@Nonnull ExecutorService executor) {
        this(new SharedExecutorFactory(executor));
    }

    /**
     * 使用指定的 {@link ExecutorFactory} 创建分发器。
     *
     * @param executorFactory 执行器工厂，不能为 null
     */
    public DefaultEventDispatcher(@Nonnull ExecutorFactory executorFactory) {
        this.executor = Lazy.of(executorFactory);
    }

    @Override
    public <E> void dispatch(@Nonnull EventInvoker invoker, @Nonnull E event) {
        if (log.isTraceEnabled()) {
            log.trace("同步分发事件到监听器: {}", invoker);
        }
        invoker.invoke(event);
    }

    @Override
    public <E> CompletableFuture<Void> dispatchAsync(@Nonnull EventInvoker invoker, @Nonnull E event) {
        if (log.isTraceEnabled()) {
            log.trace("异步分发事件到监听器: {}, 执行器: {}", invoker, this.executor.get());
        }
        return CompletableFuture.runAsync(() -> invoker.invoke(event), this.executor.get());
    }
}
