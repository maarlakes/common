package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;

/**
 * AOP 场景下从 {@link MethodInvocation} 解析出 {@link LockContext} 的策略接口。
 *
 * <p>不同的锁注解（如 {@link SyncLock}）可以有不同的解析策略。通过此接口将注解解析逻辑
 * 与拦截器逻辑解耦，使得 {@link AbstractLockMethodInterceptor} 可以复用于不同的锁注解场景。</p>
 *
 * @author linjpxc
 * @see SyncLockContextResolver
 * @see AbstractLockMethodInterceptor
 */
@FunctionalInterface
public interface LockContextResolver {

    /**
     * 从方法调用上下文中解析出完整的锁上下文。
     *
     * <p>解析过程通常包括：读取方法上的注解属性、通过 SpEL 解析动态 key、
     * 组装 {@link LockContext} 实例。</p>
     *
     * @param invocation AOP 方法调用上下文，包含目标方法、参数和目标对象
     * @return 解析后的锁上下文
     * @throws LockException 如果注解缺失或 key 解析失败
     */
    @Nonnull
    LockContext resolve(@Nonnull MethodInvocation invocation);
}
