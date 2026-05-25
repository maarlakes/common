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
 * 锁方法拦截器的公共基类，封装锁获取、方法执行和锁释放的通用逻辑。
 *
 * <p>本类实现了 {@link MethodInterceptor} 和 {@link PointcutAdvisor}，
 * 可以直接注册为 Spring AOP 的 Advisor。</p>
 *
 * <h3>锁生命周期</h3>
 * <ol>
 *     <li>通过 {@link LockContextResolver} 从方法调用中解析 {@link LockContext}</li>
 *     <li>根据方法返回类型判断使用同步锁还是异步锁</li>
 *     <li>调用 {@link #acquireLock} 获取锁</li>
 *     <li>执行目标方法</li>
 *     <li>根据同步/异步模式在适当的时机释放锁</li>
 * </ol>
 *
 * <h3>异常处理</h3>
 * <p>解锁过程中如果发生异常，不会覆盖原始的业务异常，而是通过
 * {@link Throwable#addSuppressed} 将解锁异常附加到原始异常上，保留完整的异常链。</p>
 *
 * @author linjpxc
 * @see SyncLockMethodInterceptor
 * @see LockClient
 * @see LockContextResolver
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
     * 根据等待时间策略获取锁。
     *
     * <p>三种等待策略：</p>
     * <ul>
     *     <li>{@code waitTime == 0} — 不等待，{@link Mutex#tryLock()} 立即返回，
     *         失败时抛出 {@link SyncLockTimeoutException}</li>
     *     <li>{@code waitTime > 0} — 超时等待，在指定时间内尝试获取锁，
     *         超时后抛出 {@link SyncLockTimeoutException}</li>
     *     <li>{@code waitTime < 0}（默认）— 无限等待，阻塞直到获取锁或被中断</li>
     * </ul>
     *
     * @param mutex   要获取的锁
     * @param waitTime 等待时间（毫秒），-1 无限等待，0 不等待
     * @throws SyncLockTimeoutException 如果获取锁超时或立即失败
     * @throws InterruptedException     如果等待锁时被中断
     */
    protected void acquireLock(@Nonnull Mutex mutex, long waitTime) throws InterruptedException {
        if (waitTime == 0) {
            if (!mutex.tryLock()) {
                log.warn("获取锁 [{}] 失败（不等待模式）", mutex.key());
                throw new SyncLockTimeoutException();
            }
        } else if (waitTime > 0) {
            if (!mutex.tryLock(waitTime, TimeUnit.MILLISECONDS)) {
                log.warn("获取锁 [{}] 超时，等待了 {}ms", mutex.key(), waitTime);
                throw new SyncLockTimeoutException();
            }
        } else {
            if (log.isTraceEnabled()) {
                log.trace("阻塞获取锁 [{}]", mutex.key());
            }
            mutex.lockInterruptibly();
        }
        if (log.isTraceEnabled()) {
            log.trace("获取锁 [{}] 成功，异步={}", mutex.key(), mutex.isAsync());
        }
    }

    /**
     * 根据返回类型判断是否为异步方法。
     *
     * <p>当方法返回类型为以下之一时，判定为异步方法，使用异步锁（支持跨线程解锁）：</p>
     * <ul>
     *     <li>{@link CompletionStage} — 异步编程，如 {@link java.util.concurrent.CompletableFuture}</li>
     *     <li>{@link Callable} — 延迟执行的任务</li>
     *     <li>{@link Runnable} — 无返回值的任务</li>
     *     <li>{@link Supplier} — 延迟计算</li>
     * </ul>
     *
     * @param returnType 方法的返回类型
     * @return {@code true} 表示异步方法
     */
    protected static boolean isAsyncReturnType(Class<?> returnType) {
        return CompletionStage.class.isAssignableFrom(returnType)
                || Callable.class.isAssignableFrom(returnType)
                || Runnable.class.isAssignableFrom(returnType)
                || Supplier.class.isAssignableFrom(returnType);
    }

    /**
     * 根据异步标志获取对应的 Mutex 实例。
     *
     * @param context 锁上下文
     * @param async   是否需要异步锁
     * @return 对应类型的 Mutex 实例
     */
    protected Mutex getMutex(@Nonnull LockContext context, boolean async) {
        return async ? this.lockClient.getAsyncMutex(context) : this.lockClient.getMutex(context);
    }

    /**
     * 执行目标方法并根据异步模式处理解锁。
     *
     * <p>同步模式：在 finally 块中释放锁，确保即使方法抛出异常也能解锁。</p>
     * <p>异步模式：将解锁操作注册到异步结果的完成回调中，确保异步操作执行完毕后才解锁。
     * 如果异步结果创建阶段就抛出异常（方法体本身的异常），立即释放锁。</p>
     *
     * @param invocation 方法调用上下文
     * @param mutex      当前持有的锁
     * @param async      是否异步模式
     * @return 方法执行结果（可能包装了解锁逻辑）
     * @throws Throwable 方法执行或锁操作中抛出的任何异常
     */
    protected static Object executeWithUnlock(@Nonnull MethodInvocation invocation,
                                              @Nonnull Mutex mutex, boolean async) throws Throwable {
        if (log.isTraceEnabled()) {
            log.trace("开始执行持有锁 [{}] 的方法，异步={}", mutex.key(), async);
        }

        if (async) {
            final Object result;
            try {
                result = invocation.proceed();
            } catch (Throwable t) {
                log.error("持有锁 [{}] 期间创建异步结果时发生异常，立即释放锁", mutex.key(), t);
                safeUnlock(mutex, t);
                throw t;
            }
            return handleAsyncUnlock(result, mutex);
        }

        // 同步模式：try-finally 确保解锁
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

    /**
     * 安全地释放锁，捕获并记录解锁过程中的异常。
     *
     * <p>如果解锁过程中发生异常，不覆盖原始异常，而是通过
     * {@link Throwable#addSuppressed(Throwable)} 将解锁异常附加到原始异常上。
     * 这样调用方可以在捕获原始异常时同时看到解锁异常，便于排查问题。</p>
     *
     * @param mutex   要释放的锁
     * @param primary 方法执行时的原始异常（可能为 null）
     */
    private static void safeUnlock(@Nonnull Mutex mutex, Throwable primary) {
        try {
            mutex.unlock();
            if (log.isTraceEnabled()) {
                log.trace("锁 [{}] 已释放", mutex.key());
            }
        } catch (Throwable unlockError) {
            log.warn("释放锁 [{}] 时发生异常", mutex.key(), unlockError);
            if (primary != null) {
                // 将解锁异常附加到原始异常上，保留完整的异常链
                primary.addSuppressed(unlockError);
            }
        }
    }

    /**
     * 根据异步结果的类型注册解锁回调。
     *
     * <p>不同类型有不同解锁时机：</p>
     * <ul>
     *     <li>{@link CompletionStage} — 在 {@code whenComplete} 回调中解锁，
     *         无论成功或异常都会触发</li>
     *     <li>{@link Callable} — 包装为新的 Callable，在 call() 方法的 finally 中解锁</li>
     *     <li>{@link Runnable} — 包装为新的 Runnable，在 run() 方法的 finally 中解锁</li>
     *     <li>{@link Supplier} — 包装为新的 Supplier，在 get() 方法的 finally 中解锁</li>
     *     <li>其他类型 — 视为同步结果，立即解锁</li>
     * </ul>
     *
     * @param result 原始方法返回值
     * @param mutex  当前持有的锁
     * @return 可能经过包装的返回值
     */
    private static Object handleAsyncUnlock(@Nonnull Object result, @Nonnull Mutex mutex) {
        if (result instanceof CompletionStage) {
            if (log.isTraceEnabled()) {
                log.trace("异步锁 [{}] 匹配 CompletionStage 类型，注册 whenComplete 回调解锁", mutex.key());
            }
            return ((CompletionStage<?>) result).whenComplete((v, ex) -> {
                safeUnlock(mutex, ex);
            });
        } else if (result instanceof Callable) {
            if (log.isTraceEnabled()) {
                log.trace("异步锁 [{}] 匹配 Callable 类型，包装延迟解锁", mutex.key());
            }
            return (Callable<?>) () -> {
                try {
                    return ((Callable<?>) result).call();
                } finally {
                    safeUnlock(mutex, null);
                }
            };
        } else if (result instanceof Runnable) {
            if (log.isTraceEnabled()) {
                log.trace("异步锁 [{}] 匹配 Runnable 类型，包装延迟解锁", mutex.key());
            }
            return (Runnable) () -> {
                try {
                    ((Runnable) result).run();
                } finally {
                    safeUnlock(mutex, null);
                }
            };
        } else if (result instanceof Supplier) {
            if (log.isTraceEnabled()) {
                log.trace("异步锁 [{}] 匹配 Supplier 类型，包装延迟解锁", mutex.key());
            }
            return (Supplier<?>) () -> {
                try {
                    return ((Supplier<?>) result).get();
                } finally {
                    safeUnlock(mutex, null);
                }
            };
        }

        // 非 async 返回类型的实际值（如方法签名是 Object 但返回了普通值），立即解锁
        if (log.isTraceEnabled()) {
            log.trace("异步锁 [{}] 返回值非异步类型，立即解锁", mutex.key());
        }
        safeUnlock(mutex, null);
        return result;
    }
}
