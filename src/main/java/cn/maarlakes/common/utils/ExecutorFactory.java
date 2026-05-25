package cn.maarlakes.common.utils;

import cn.maarlakes.common.function.Function0;

import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
public interface ExecutorFactory extends Function0<Executor> {

    @Override
    default Executor apply() throws Exception {
        return this.createExecutor();
    }

    Executor createExecutor();
}
