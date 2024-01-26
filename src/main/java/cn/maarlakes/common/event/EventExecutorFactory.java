package cn.maarlakes.common.event;

import jakarta.annotation.Nonnull;

import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
public interface EventExecutorFactory {

    @Nonnull
    Executor getExecutor();
}
