package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;

import java.util.concurrent.locks.Lock;

/**
 * @author linjpxc
 */
public interface SerialTaskExecuteStrategy {

    Object execute(String taskName, @Nonnull MethodInvocation invocation, @Nonnull Lock lock);
}
