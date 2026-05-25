package cn.maarlakes.common.task;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;

/**
 * 串行任务获取锁超时时的执行策略。
 *
 * <p>当 {@link SerialTask} 注解的方法在 {@code waitTime} 内未能获取到锁时，
 * 框架会调用此策略来决定如何处理。默认实现 {@link IgnoredSerialTaskExecuteStrategy}
 * 直接返回 {@code null}，即放弃本次方法调用。</p>
 *
 * <p>自定义策略实现类可以通过 Spring 容器管理（通过 {@link cn.maarlakes.common.factory.bean.BeanFactories} 获取），
 * 从而在超时时执行降级逻辑、记录告警或抛出特定异常。</p>
 *
 * @author linjpxc
 * @see IgnoredSerialTaskExecuteStrategy
 * @see SerialTask#strategy()
 */
public interface SerialTaskExecuteStrategy {

    /**
     * 在锁获取超时后执行降级逻辑。
     *
     * <p>返回值将作为被拦截方法的返回值。如果方法返回类型为基本类型，
     * 拦截器会将 {@code null} 替换为对应基本类型的默认值（如 {@code 0}、{@code false}）。</p>
     *
     * @param taskName   任务名称（锁 key），用于日志上下文
     * @param invocation 被拦截的方法调用
     * @return 降级返回值，可以为 {@code null}
     */
    Object execute(String taskName, @Nonnull MethodInvocation invocation);
}
