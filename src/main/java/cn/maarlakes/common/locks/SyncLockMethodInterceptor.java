package cn.maarlakes.common.locks;

import cn.maarlakes.common.utils.PointcutUtils;
import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

/**
 * {@link SyncLock} 注解的方法拦截器，Spring AOP 的 {@link org.aopalliance.intercept.MethodInterceptor} 实现。
 *
 * <p>当标注了 {@link SyncLock} 的方法被调用时，拦截器自动执行以下流程：</p>
 * <ol>
 *     <li>通过 {@link SyncLockContextResolver} 从注解中解析 {@link LockContext}</li>
 *     <li>根据方法返回类型自动判断锁模式：
 *         <ul>
 *             <li>返回 {@link java.util.concurrent.CompletionStage} / {@link java.util.concurrent.Callable} /
 *                 {@link java.lang.Runnable} / {@link java.util.function.Supplier} → 异步锁，延迟解锁</li>
 *             <li>其他返回类型 → 同步锁，方法执行完毕后立即解锁</li>
 *         </ul>
 *     </li>
 *     <li>获取锁</li>
 *     <li>执行目标方法</li>
 *     <li>释放锁（同步模式立即释放，异步模式在异步操作完成后释放）</li>
 * </ol>
 *
 * <h3>注册方式</h3>
 * <p>作为 {@link org.springframework.aop.PointcutAdvisor} 注册为 Spring Bean，
 * 切点自动匹配标注了 {@link SyncLock} 的方法。</p>
 *
 * @author linjpxc
 * @see SyncLock
 * @see AbstractLockMethodInterceptor
 * @see SyncLockContextResolver
 */
public class SyncLockMethodInterceptor extends AbstractLockMethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SyncLockMethodInterceptor.class);

    /**
     * 创建拦截器，使用默认的 {@link SyncLockContextResolver}。
     *
     * @param lockClient 锁客户端
     */
    public SyncLockMethodInterceptor(@Nonnull LockClient lockClient) {
        this(lockClient, new SyncLockContextResolver());
    }

    /**
     * 创建拦截器，使用自定义的 {@link LockContextResolver}。
     *
     * @param lockClient 锁客户端
     * @param resolver   锁上下文解析器
     */
    public SyncLockMethodInterceptor(@Nonnull LockClient lockClient, @Nonnull LockContextResolver resolver) {
        super(lockClient, resolver, PointcutUtils.forAnnotations(SyncLock.class));
    }

    /**
     * 拦截标注了 {@link SyncLock} 的方法调用。
     *
     * <p>执行流程：解析锁上下文 → 判断同步/异步模式 → 获取锁 → 执行方法 → 释放锁。</p>
     *
     * <p>方法开头的 {@link Assert#state} 是防御性检查：正常情况下切点已确保方法上有
     * {@link SyncLock} 注解，此断言不应触发。如果触发，说明切点配置有误。</p>
     *
     * @param invocation 方法调用上下文
     * @return 方法执行结果
     * @throws Throwable 方法执行或锁操作中抛出的任何异常
     */
    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        // 防御性检查：切点应保证注解存在
        Assert.state(AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SyncLock.class) != null,
                "SyncLock annotation not found");

        final LockContext context = this.resolver.resolve(invocation);
        final boolean async = isAsyncReturnType(invocation.getMethod().getReturnType());

        if (log.isTraceEnabled()) {
            log.trace("拦截 @SyncLock 方法：{}，异步模式={}", invocation.getMethod(), async);
        }

        final Mutex mutex = getMutex(context, async);
        this.acquireLock(mutex, context.waitTime());

        if (log.isDebugEnabled()) {
            log.debug("@SyncLock 方法 [{}] 锁已获取，异步={}", mutex.key(), async);
        }

        return executeWithUnlock(invocation, mutex, async);
    }
}
