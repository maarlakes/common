package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author linjpxc
 */
public class DefaultEventDispatcher implements EventDispatcher {

    @Nonnull
    private final EventExecutorFactory executorFactory;

    public DefaultEventDispatcher(int threads) {
        this(new ThreadPoolExecutor(threads, threads, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>()));
    }

    public DefaultEventDispatcher(@Nonnull ExecutorService executor) {
        this(new DefaultEventExecutorFactory(executor));
    }

    public DefaultEventDispatcher(@Nonnull EventExecutorFactory executorFactory) {
        this.executorFactory = executorFactory;
    }

    @Override
    public <E> void dispatch(@Nonnull EventInvoker invoker, @Nonnull E event) {
        if (invoker.supportedAsync()) {
            this.executorFactory.getExecutor().execute(() -> invoker.invoke(event));
        } else {
            invoker.invoke(event);
        }
    }
}
