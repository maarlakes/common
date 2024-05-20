package cn.maarlakes.common.locks;

import cn.maarlakes.common.utils.PointcutUtils;
import jakarta.annotation.Nonnull;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.function.Supplier;

/**
 * @author linjpxc
 */
public class SyncLockMethodInterceptor implements MethodInterceptor, PointcutAdvisor, AopInfrastructureBean {
    private final Pointcut pointcut;
    private final LockClient lockClient;
    private final LockContextFactory lockContextFactory;

    public SyncLockMethodInterceptor(@Nonnull LockClient lockClient) {
        this(lockClient, new ExpressionLockContextFactory());
    }

    public SyncLockMethodInterceptor(@Nonnull LockClient lockClient, @Nonnull LockContextFactory lockContextFactory) {
        this.lockClient = Objects.requireNonNull(lockClient);
        this.lockContextFactory = Objects.requireNonNull(lockContextFactory);
        this.pointcut = PointcutUtils.forAnnotations(SyncLock.class);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final SyncLock syncLock = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SyncLock.class);
        Assert.state(syncLock != null, "SyncLock is null.");

        String value = syncLock.value();
        if (value == null || value.isEmpty()) {
            value = "'" + invocation.getMethod() + "'";
        }
        final Lock lock = this.lockClient.createLock(this.lockContextFactory.create(value, invocation));
        if (syncLock.timeout() > 0) {
            if (!lock.tryLock(syncLock.timeout(), TimeUnit.MILLISECONDS)) {
                throw new SyncLockTimeoutException("method: " + invocation.getMethod());
            }
        } else {
            lock.lock();
        }
        return invokeUnlock(syncLock, invocation.proceed(), lock);
    }

    @Nonnull
    @Override
    public Pointcut getPointcut() {
        return this.pointcut;
    }

    @Nonnull
    @Override
    public Advice getAdvice() {
        return this;
    }

    @Override
    public boolean isPerInstance() {
        return true;
    }

    private static Object invokeUnlock(@Nonnull SyncLock syncLock, @Nonnull Object result, @Nonnull Lock lock) {
        if (syncLock.supportAsync()) {
            if (result instanceof CompletionStage) {
                return ((CompletionStage<?>) result).whenComplete((v, r) -> lock.unlock());
            } else if (result instanceof Callable) {
                return (Callable<?>) () -> {
                    try {
                        return ((Callable<?>) result).call();
                    } finally {
                        lock.unlock();
                    }
                };
            } else if (result instanceof Runnable) {
                return (Runnable) () -> {
                    try {
                        ((Runnable) result).run();
                    } finally {
                        lock.unlock();
                    }
                };
            } else if (result instanceof Supplier) {
                return (Supplier<?>) () -> {
                    try {
                        return ((Supplier<?>) result).get();
                    } finally {
                        lock.unlock();
                    }
                };
            }
        }
        try {
            return result;
        } finally {
            lock.unlock();
        }
    }
}
