package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.AopInfrastructureBean;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 锁方法拦截器的公共基类，封装锁获取和释放的通用逻辑。
 *
 * @author linjpxc
 */
public abstract class AbstractLockMethodInterceptor implements MethodInterceptor, PointcutAdvisor, AopInfrastructureBean {

    private static final Logger log = LoggerFactory.getLogger(AbstractLockMethodInterceptor.class);

    protected final LockClient lockClient;
    protected final LockContextResolver resolver;
    protected final Pointcut pointcut;

    protected AbstractLockMethodInterceptor(@Nonnull LockClient lockClient,
                                            @Nonnull LockContextResolver resolver,
                                            @Nonnull Pointcut pointcut) {
        this.lockClient = lockClient;
        this.resolver = resolver;
        this.pointcut = pointcut;
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

    /**
     * 获取锁。
     *
     * @throws SyncLockTimeoutException 如果获取锁超时
     * @throws InterruptedException     如果等待锁时被中断
     */
    protected void acquireLock(@Nonnull Mutex mutex, long waitTime) throws InterruptedException {
        if (waitTime == 0) {
            if (!mutex.tryLock()) {
                log.warn("获取锁 [{}] 失败，立即返回", mutex.key());
                throw new SyncLockTimeoutException();
            }
        } else if (waitTime > 0) {
            if (!mutex.tryLock(waitTime, TimeUnit.MILLISECONDS)) {
                log.warn("获取锁 [{}] 超时，等待了 {}ms", mutex.key(), waitTime);
                throw new SyncLockTimeoutException();
            }
        } else {
            mutex.lockInterruptibly();
        }
        log.debug("获取锁 [{}] 成功，异步={}", mutex.key(), mutex.isAsync());
    }

    /**
     * 根据返回类型判断是否为异步方法。
     */
    protected static boolean isAsyncReturnType(Class<?> returnType) {
        return CompletionStage.class.isAssignableFrom(returnType)
                || Callable.class.isAssignableFrom(returnType)
                || Runnable.class.isAssignableFrom(returnType)
                || Supplier.class.isAssignableFrom(returnType);
    }

    /**
     * 根据异步标志获取对应的 Mutex。
     */
    protected Mutex getMutex(@Nonnull LockContext context, boolean async) {
        return async ? this.lockClient.getAsyncMutex(context) : this.lockClient.getMutex(context);
    }

    /**
     * 执行目标方法并根据异步模式处理解锁。
     */
    protected static Object executeWithUnlock(@Nonnull MethodInvocation invocation,
                                              @Nonnull Mutex mutex, boolean async) throws Throwable {
        if (async) {
            final Object result;
            try {
                result = invocation.proceed();
            } catch (Throwable t) {
                log.error("持有锁 [{}] 期间发生异常，释放锁", mutex.key(), t);
                safeUnlock(mutex, t);
                throw t;
            }
            return handleAsyncUnlock(result, mutex);
        }

        Throwable primary = null;
        try {
            return invocation.proceed();
        } catch (Throwable t) {
            primary = t;
            throw t;
        } finally {
            safeUnlock(mutex, primary);
        }
    }

    private static void safeUnlock(@Nonnull Mutex mutex, Throwable primary) {
        try {
            mutex.unlock();
            log.debug("锁 [{}] 已释放", mutex.key());
        } catch (Throwable unlockError) {
            log.warn("释放锁 [{}] 异常", mutex.key(), unlockError);
            if (primary != null) {
                primary.addSuppressed(unlockError);
            }
        }
    }

    private static Object handleAsyncUnlock(@Nonnull Object result, @Nonnull Mutex mutex) {
        if (result instanceof CompletionStage) {
            return ((CompletionStage<?>) result).whenComplete((v, ex) -> {
                safeUnlock(mutex, ex);
            });
        } else if (result instanceof Callable) {
            return (Callable<?>) () -> {
                try {
                    return ((Callable<?>) result).call();
                } finally {
                    safeUnlock(mutex, null);
                }
            };
        } else if (result instanceof Runnable) {
            return (Runnable) () -> {
                try {
                    ((Runnable) result).run();
                } finally {
                    safeUnlock(mutex, null);
                }
            };
        } else if (result instanceof Supplier) {
            return (Supplier<?>) () -> {
                try {
                    return ((Supplier<?>) result).get();
                } finally {
                    safeUnlock(mutex, null);
                }
            };
        }

        // 同步结果，立即解锁
        safeUnlock(mutex, null);
        return result;
    }
}
