package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 忽略执行策略，直接返回 null。
 *
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
    public Object execute(String ignored, @Nonnull MethodInvocation invocation) {
        return null;
    }

    private static final class Helper {
        public static final IgnoredSerialTaskExecuteStrategy INSTANCE = new IgnoredSerialTaskExecuteStrategy();
    }
}
