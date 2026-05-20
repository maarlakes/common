package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 串行任务获取锁超时时的执行策略。
 *
 * @author linjpxc
 */
public interface SerialTaskExecuteStrategy {

    Object execute(String taskName, @Nonnull MethodInvocation invocation);
}
