package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;

import java.util.concurrent.locks.Lock;

/**
 * @author linjpxc
 */
public final class IgnoredSerialTaskExecuteStrategy implements SerialTaskExecuteStrategy {

    private IgnoredSerialTaskExecuteStrategy() {
    }

    @Nonnull
    public static IgnoredSerialTaskExecuteStrategy getInstance() {
        return Helper.INSTANCE;
    }

    @Override
    public Object execute(String ignored, @Nonnull MethodInvocation invocation, @Nonnull Lock lock) {
        return null;
    }

    private static final class Helper {
        public static final IgnoredSerialTaskExecuteStrategy INSTANCE = new IgnoredSerialTaskExecuteStrategy();
    }
}
