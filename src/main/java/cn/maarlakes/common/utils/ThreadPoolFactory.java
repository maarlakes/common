package cn.maarlakes.common.utils;


import java.util.concurrent.Executor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author linjpxc
 */
public class ThreadPoolFactory implements ExecutorFactory {

    private final ThreadPoolConfig config;

    public ThreadPoolFactory(ThreadPoolConfig config) {
        this.config = config;
    }

    @Override
    public Executor createExecutor() {
        final ThreadPoolExecutor executor = new ThreadPoolExecutor(
                this.config.getCoreSize(),
                this.config.getMaximumSize(),
                this.config.getKeepAliveTime().toMillis(),
                TimeUnit.MILLISECONDS,
                this.config.getQueue() == null ? new SynchronousQueue<>() : this.config.getQueue(),
                new NamedThreadFactory(this.config.getThreadNamePrefix()),
                this.config.getRejectedHandler() == null ? new ThreadPoolExecutor.AbortPolicy() : this.config.getRejectedHandler()
        );
        executor.allowCoreThreadTimeOut(true);
        return executor;
    }
}
