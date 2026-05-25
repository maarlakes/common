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
 * {@link SerialTask} 注解的方法拦截器，实现方法级别的串行化执行。
 *
 * <p>拦截流程：</p>
 * <ol>
 *     <li>查找方法上的 {@code @SerialTask} 注解</li>
 *     <li>根据方法返回类型判断同步/异步模式：
 *         返回 {@code CompletionStage}/{@code Callable}/{@code Runnable}/{@code Supplier} 时为异步模式</li>
 *     <li>尝试在 {@code waitTime} 内获取锁</li>
 *     <li>获取成功 → 执行方法，完成后释放锁</li>
 *     <li>获取超时 → 执行 {@link SerialTask#strategy()} 指定的降级策略</li>
 * </ol>
 *
 * @author linjpxc
 * @see SerialTask
 * @see AbstractLockMethodInterceptor
 */
public class SerialTaskMethodInterceptor extends AbstractLockMethodInterceptor {

    private static final Logger log = LoggerFactory.getLogger(SerialTaskMethodInterceptor.class);

    /**
     * 使用默认的 {@link SerialTaskContextResolver} 创建拦截器。
     *
     * @param lockClient 锁客户端
     */
    public SerialTaskMethodInterceptor(@Nonnull LockClient lockClient) {
        this(lockClient, new SerialTaskContextResolver());
    }

    /**
     * 使用自定义的锁上下文解析器创建拦截器。
     *
     * @param lockClient 锁客户端
     * @param resolver   锁上下文解析器
     */
    public SerialTaskMethodInterceptor(@Nonnull LockClient lockClient, @Nonnull LockContextResolver resolver) {
        super(lockClient, resolver, PointcutUtils.forAnnotations(SerialTask.class));
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final SerialTask serialTask = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SerialTask.class);
        Assert.state(serialTask != null, "SerialTask annotation not found");

        final LockContext context = this.resolver.resolve(invocation);
        // 根据方法返回类型判断是否为异步模式
        final boolean async = isAsyncReturnType(invocation.getMethod().getReturnType());
        final Mutex mutex = getMutex(context, async);

        if (log.isTraceEnabled()) {
            log.trace("串行任务拦截，方法：{}，锁 key：{}，异步模式：{}", invocation.getMethod(), context.key(), async);
        }

        try {
            this.acquireLock(mutex, context.waitTime());
        } catch (SyncLockTimeoutException e) {
            // 获取锁超时，执行降级策略
            log.warn("串行任务 [{}] 获取锁超时，执行策略：{}", context.key(), serialTask.strategy().getSimpleName());
            final SerialTaskExecuteStrategy strategy = getSerialTaskExecuteStrategy(serialTask.strategy());
            if (log.isDebugEnabled()) {
                log.debug("串行任务 [{}] 使用降级策略：{}", context.key(), strategy.getClass().getName());
            }
            final Object result = strategy.execute(context.key(), invocation);
            // 基本类型返回值不能为 null，需要替换为默认值
            if (result == null && invocation.getMethod().getReturnType().isPrimitive()) {
                return defaultPrimitiveValue(invocation.getMethod().getReturnType());
            }
            return result;
        }

        return executeWithUnlock(invocation, mutex, async);
    }

    /**
     * 获取串行任务执行策略实例。
     *
     * <p>对于 {@link IgnoredSerialTaskExecuteStrategy} 直接返回单例，
     * 其他策略类型通过 {@link BeanFactories} 从容器获取。</p>
     */
    private static SerialTaskExecuteStrategy getSerialTaskExecuteStrategy(Class<? extends SerialTaskExecuteStrategy> type) {
        if (type == IgnoredSerialTaskExecuteStrategy.class) {
            return IgnoredSerialTaskExecuteStrategy.getInstance();
        }
        if (log.isTraceEnabled()) {
            log.trace("加载自定义串行任务执行策略：{}", type.getName());
        }
        return BeanFactories.getBean(type);
    }

    /**
     * 返回 Java 基本类型的默认值。
     *
     * <p>当降级策略返回 {@code null} 但方法返回类型为基本类型时使用。</p>
     */
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
