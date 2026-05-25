package cn.maarlakes.common.utils;


import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
public class SharedExecutorFactory implements ExecutorFactory {

    private final Executor executor;

    public SharedExecutorFactory(Executor executor) {
        this.executor = executor;
    }

    @Override
    public Executor createExecutor() {
        return this.executor;
    }
}
