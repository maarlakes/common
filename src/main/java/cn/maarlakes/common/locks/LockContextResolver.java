package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;

/**
 * AOP 场景下从 {@link MethodInvocation} 解析出 {@link LockContext}。
 *
 * @author linjpxc
 */
@FunctionalInterface
public interface LockContextResolver {

    /**
     * 从方法调用上下文中解析出完整的锁上下文。
     */
    @Nonnull
    LockContext resolve(@Nonnull MethodInvocation invocation);
}
