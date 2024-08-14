package cn.maarlakes.common.utils;

import cn.maarlakes.common.function.Function0;
import jakarta.annotation.Nonnull;

import java.util.concurrent.Executor;

/**
 * @author linjpxc
 */
public interface ExecutorFactory extends Function0<Executor> {

    @Nonnull
    @Override
    default Executor apply() throws Exception {
        return this.createExecutor();
    }

    @Nonnull
    Executor createExecutor();
}
