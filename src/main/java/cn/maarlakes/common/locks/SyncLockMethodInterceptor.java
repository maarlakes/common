package cn.maarlakes.common.locks;

import cn.maarlakes.common.utils.PointcutUtils;
import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

/**
 * {@link SyncLock} 注解的方法拦截器。
 *
 * <p>根据方法返回类型自动选择锁模式：</p>
 * <ul>
 *     <li>返回 CompletionStage/Callable/Runnable/Supplier → 异步锁，延迟解锁</li>
 *     <li>其他返回类型 → 同步锁，立即解锁</li>
 * </ul>
 *
 * @author linjpxc
 */
public class SyncLockMethodInterceptor extends AbstractLockMethodInterceptor {

    public SyncLockMethodInterceptor(@Nonnull LockClient lockClient) {
        this(lockClient, new SyncLockContextResolver());
    }

    public SyncLockMethodInterceptor(@Nonnull LockClient lockClient, @Nonnull LockContextResolver resolver) {
        super(lockClient, resolver, PointcutUtils.forAnnotations(SyncLock.class));
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        Assert.state(AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SyncLock.class) != null,
                "SyncLock annotation not found");

        final LockContext context = this.resolver.resolve(invocation);
        final boolean async = isAsyncReturnType(invocation.getMethod().getReturnType());
        final Mutex mutex = getMutex(context, async);

        this.acquireLock(mutex, context.waitTime());
        return executeWithUnlock(invocation, mutex, async);
    }
}
