package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

/**
 * @author linjpxc
 */
public class DefaultEventExecutorFactory implements EventExecutorFactory {

    private final ExecutorService executorService;

    public DefaultEventExecutorFactory(@Nonnull final ExecutorService executorService) {
        this.executorService = executorService;
    }

    @Nonnull
    @Override
    public Executor getExecutor() {
        return this.executorService;
    }
}
