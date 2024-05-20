package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;

/**
 * @author linjpxc
 */
public interface LockContextFactory {

    LockContext create(@Nonnull String key, @Nonnull MethodInvocation invocation);
}
