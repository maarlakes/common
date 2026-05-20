package cn.maarlakes.common.task;

import cn.maarlakes.common.factory.bean.BeanFactories;
import cn.maarlakes.common.locks.*;
import cn.maarlakes.common.utils.PointcutUtils;
import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

/**
 * {@link SerialTask} 注解的方法拦截器。
 *
 * <p>根据方法返回类型自动选择锁模式：</p>
 * <ul>
 *     <li>返回 CompletionStage/Callable/Runnable/Supplier → 异步锁，延迟解锁</li>
 *     <li>其他返回类型 → 同步锁，立即解锁</li>
 * </ul>
 *
 * @author linjpxc
 */
public class SerialTaskMethodInterceptor extends AbstractLockMethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SerialTaskMethodInterceptor.class);

    public SerialTaskMethodInterceptor(@Nonnull LockClient lockClient) {
        this(lockClient, new SerialTaskContextResolver());
    }

    public SerialTaskMethodInterceptor(@Nonnull LockClient lockClient, @Nonnull LockContextResolver resolver) {
        super(lockClient, resolver, PointcutUtils.forAnnotations(SerialTask.class));
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final SerialTask serialTask = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SerialTask.class);
        Assert.state(serialTask != null, "SerialTask annotation not found");

        final LockContext context = this.resolver.resolve(invocation);
        final boolean async = isAsyncReturnType(invocation.getMethod().getReturnType());
        final Mutex mutex = getMutex(context, async);

        try {
            this.acquireLock(mutex, context.waitTime());
        } catch (SyncLockTimeoutException e) {
            log.warn("串行任务 [{}] 获取锁超时，执行策略：{}", context.key(), serialTask.strategy().getSimpleName());
            final Object result = getSerialTaskExecuteStrategy(serialTask.strategy())
                    .execute(context.key(), invocation);
            if (result == null && invocation.getMethod().getReturnType().isPrimitive()) {
                return defaultPrimitiveValue(invocation.getMethod().getReturnType());
            }
            return result;
        }

        return executeWithUnlock(invocation, mutex, async);
    }

    private static SerialTaskExecuteStrategy getSerialTaskExecuteStrategy(Class<? extends SerialTaskExecuteStrategy> type) {
        if (type == IgnoredSerialTaskExecuteStrategy.class) {
            return IgnoredSerialTaskExecuteStrategy.getInstance();
        }
        return BeanFactories.getBean(type);
    }

    private static Object defaultPrimitiveValue(Class<?> primitiveType) {
        if (primitiveType == boolean.class) {
            return false;
        }
        if (primitiveType == byte.class) {
            return (byte) 0;
        }
        if (primitiveType == char.class) {
            return '\0';
        }
        if (primitiveType == short.class) {
            return (short) 0;
        }
        if (primitiveType == int.class) {
            return 0;
        }
        if (primitiveType == long.class) {
            return 0L;
        }
        if (primitiveType == float.class) {
            return 0.0f;
        }
        if (primitiveType == double.class) {
            return 0.0d;
        }
        return null;
    }
}
