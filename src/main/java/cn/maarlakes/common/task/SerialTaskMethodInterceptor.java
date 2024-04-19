package cn.maarlakes.common.task;

import cn.maarlakes.common.factory.bean.BeanFactories;
import cn.maarlakes.common.locks.LockContext;
import cn.maarlakes.common.locks.LockFactory;
import cn.maarlakes.common.utils.PointcutUtils;
import jakarta.annotation.Nonnull;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.util.Assert;

import java.lang.reflect.Method;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author linjpxc
 */
public class SerialTaskMethodInterceptor implements MethodInterceptor, PointcutAdvisor, AopInfrastructureBean {

    private static final Logger log = LoggerFactory.getLogger(SerialTaskMethodInterceptor.class);

    private final Pointcut pointcut;
    private final LockFactory lockFactory;

    public SerialTaskMethodInterceptor(@Nonnull LockFactory lockFactory) {
        this.lockFactory = Objects.requireNonNull(lockFactory);
        this.pointcut = PointcutUtils.forAnnotations(SerialTask.class);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final SerialTask serialTask = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SerialTask.class);
        Assert.state(serialTask != null, "SerialTask is null.");

        final String taskName = getTaskName(serialTask, invocation.getMethod());

        final Lock lock = this.lockFactory.createLock(LockContext.create(taskName, serialTask.fair()));
        if (serialTask.timeout() > 0) {
            if (!lock.tryLock(serialTask.timeout(), TimeUnit.MILLISECONDS)) {
                return getSerialTaskExecuteStrategy(serialTask.strategy()).execute(taskName, invocation, lock);
            }
        } else {
            lock.lock();
        }
        try {
            return invocation.proceed();
        } finally {
            lock.unlock();
        }
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

    private static SerialTaskExecuteStrategy getSerialTaskExecuteStrategy(Class<? extends SerialTaskExecuteStrategy> type) {
        if (type == IgnoredSerialTaskExecuteStrategy.class) {
            return IgnoredSerialTaskExecuteStrategy.getInstance();
        }
        return BeanFactories.getBean(SerialTaskExecuteStrategy.class);
    }

    private static String getTaskName(@Nonnull SerialTask serialTask, @Nonnull Method method) {
        final String value = serialTask.value();
        if (value == null || value.isEmpty()) {
            return method.getDeclaringClass().getName() + "." + method.getName();
        }
        return value;
    }
}
