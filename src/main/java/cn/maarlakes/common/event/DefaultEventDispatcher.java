package cn.maarlakes.common.event;

import cn.maarlakes.common.utils.ExecutorFactory;
import cn.maarlakes.common.utils.Lazy;
import cn.maarlakes.common.utils.SharedExecutorFactory;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;

/**
 * @author linjpxc
 */
public class DefaultEventDispatcher implements EventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(DefaultEventDispatcher.class);

    @Nonnull
    private final Lazy<Executor> executor;

    public DefaultEventDispatcher(int threads) {
        this(new ThreadPoolExecutor(threads, threads, 1L, TimeUnit.MINUTES,
                new LinkedBlockingQueue<>(1000),
                new ThreadPoolExecutor.CallerRunsPolicy()));
    }

    public DefaultEventDispatcher(@Nonnull ExecutorService executor) {
        this(new SharedExecutorFactory(executor));
    }

    public DefaultEventDispatcher(@Nonnull ExecutorFactory executorFactory) {
        this.executor = Lazy.of(executorFactory);
    }

    @Override
    public <E> void dispatch(@Nonnull EventInvoker invoker, @Nonnull E event) {
        if (log.isTraceEnabled()) {
            log.trace("Sync dispatch to {}", invoker);
        }
        invoker.invoke(event);
    }

    @Override
    public <E> CompletableFuture<Void> dispatchAsync(@Nonnull EventInvoker invoker, @Nonnull E event) {
        if (log.isTraceEnabled()) {
            log.trace("Async dispatch to {} via executor {}", invoker, this.executor.get());
        }
        return CompletableFuture.runAsync(() -> invoker.invoke(event), this.executor.get());
    }
}
