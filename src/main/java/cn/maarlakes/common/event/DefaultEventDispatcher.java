package cn.maarlakes.common.event;

import cn.maarlakes.common.utils.ExecutorFactory;
import cn.maarlakes.common.utils.Lazy;
import cn.maarlakes.common.utils.SharedExecutorFactory;
import jakarta.annotation.Nonnull;

import java.util.concurrent.*;

/**
 * @author linjpxc
 */
public class DefaultEventDispatcher implements EventDispatcher {

    @Nonnull
    private final Lazy<Executor> executor;

    public DefaultEventDispatcher(int threads) {
        this(new ThreadPoolExecutor(threads, threads, 1L, TimeUnit.MINUTES, new LinkedBlockingQueue<>()));
    }

    public DefaultEventDispatcher(@Nonnull ExecutorService executor) {
        this(new SharedExecutorFactory(executor));
    }

    public DefaultEventDispatcher(@Nonnull ExecutorFactory executorFactory) {
        this.executor = Lazy.of(executorFactory);
    }

    @Override
    public <E> void dispatch(@Nonnull EventInvoker invoker, @Nonnull E event) {
        final EventDispatch annotation = invoker.getAnnotation(EventDispatch.class);
        if (annotation != null && annotation.async()) {
            this.executor.get().execute(() -> invoker.invoke(event));
        } else {
            invoker.invoke(event);
        }
    }
}
