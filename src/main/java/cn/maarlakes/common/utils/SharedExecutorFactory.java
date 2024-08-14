package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
public class SharedExecutorFactory implements ExecutorFactory {

    private final Executor executor;

    public SharedExecutorFactory(@Nonnull Executor executor) {
        this.executor = executor;
    }

    @Nonnull
    @Override
    public Executor createExecutor() {
        return this.executor;
    }
}
