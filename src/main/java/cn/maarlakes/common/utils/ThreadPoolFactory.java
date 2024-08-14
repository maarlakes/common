package cn.maarlakes.common.utils;

import jakarta.annotation.Nonnull;

import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author linjpxc
 */
public class ThreadPoolFactory implements ExecutorFactory {

    private final ThreadPoolConfig config;

    public ThreadPoolFactory(@Nonnull ThreadPoolConfig config) {
        this.config = config;
    }

    @Nonnull
    @Override
    public Executor createExecutor() {
        return new ThreadPoolExecutor(
                this.config.getCoreSize(),
                this.config.getMaximumSize(),
                this.config.getKeepAliveTime().toMillis(),
                TimeUnit.MILLISECONDS,
                this.config.getQueue() == null ? new SynchronousQueue<>() : this.config.getQueue(),
                new NamedThreadFactory(this.config.getThreadNamePrefix())
        );
    }
}
