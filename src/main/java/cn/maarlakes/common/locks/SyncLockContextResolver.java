package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 从 {@link cn.maarlakes.common.locks.SyncLock} 注解中解析 {@link LockContext}。
 *
 * @author linjpxc
 */
public class SyncLockContextResolver implements LockContextResolver {

    @Nonnull
    @Override
    public LockContext resolve(@Nonnull MethodInvocation invocation) {
        final SyncLock syncLock = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SyncLock.class);
        if (syncLock == null) {
            throw new LockException("SyncLock annotation not found on method: " + invocation.getMethod());
        }

        final String key = SpelLockKeyResolver.resolveKey(syncLock.value(), invocation);
        return LockContext.create(key, syncLock.fair(), syncLock.waitTime(), syncLock.leaseTime());
    }
}
