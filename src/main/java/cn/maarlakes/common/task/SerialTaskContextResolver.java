package cn.maarlakes.common.task;

import cn.maarlakes.common.locks.LockContext;
import cn.maarlakes.common.locks.LockContextResolver;
import cn.maarlakes.common.locks.LockException;
import cn.maarlakes.common.locks.SpelLockKeyResolver;
import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;

/**
 * 从 {@link SerialTask} 注解中解析 {@link LockContext}。
 *
 * @author linjpxc
 */
public class SerialTaskContextResolver implements LockContextResolver {

    @Nonnull
    @Override
    public LockContext resolve(@Nonnull MethodInvocation invocation) {
        final SerialTask serialTask = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SerialTask.class);
        if (serialTask == null) {
            throw new LockException("SerialTask annotation not found on method: " + invocation.getMethod());
        }

        final String key = SpelLockKeyResolver.resolveKey(serialTask.value(), invocation);
        return LockContext.create(key, serialTask.fair(), serialTask.waitTime(), serialTask.leaseTime());
    }
}
