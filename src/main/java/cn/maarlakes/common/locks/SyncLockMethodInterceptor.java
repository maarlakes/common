package cn.maarlakes.common.locks;

import cn.maarlakes.common.factory.bean.BeanFactories;
import cn.maarlakes.common.utils.PointcutUtils;
import jakarta.annotation.Nonnull;
import org.aopalliance.aop.Advice;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.aop.Pointcut;
import org.springframework.aop.PointcutAdvisor;
import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.util.Assert;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;

/**
 * @author linjpxc
 */
public class SyncLockMethodInterceptor implements MethodInterceptor, PointcutAdvisor, AopInfrastructureBean {
    private final ParameterNameDiscoverer parameterNameDiscoverer = new StandardReflectionParameterNameDiscoverer();
    private final Pointcut pointcut;
    private final LockClient lockClient;

    public SyncLockMethodInterceptor(@Nonnull LockClient lockClient) {
        this.lockClient = Objects.requireNonNull(lockClient);
        this.pointcut = PointcutUtils.forAnnotations(SyncLock.class);
    }

    @Override
    public Object invoke(MethodInvocation invocation) throws Throwable {
        final SyncLock syncLock = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SyncLock.class);
        Assert.state(syncLock != null, "SyncLock is null.");

        final StandardEvaluationContext context = new StandardEvaluationContext(invocation.getThis());
        final String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(invocation.getMethod());
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], invocation.getArguments()[i]);
            }
        }
        context.setBeanResolver((c, beanName) -> BeanFactories.getBean(beanName));

        String value = syncLock.value();
        if (value == null || value.isEmpty()) {
            value = "'" + invocation.getMethod() + "'";
        }
        final Lock lock = this.lockClient.createLock(ExpressionLockContext.create(context, value, syncLock.fair()));
        if (syncLock.timeout() > 0) {
            if (!lock.tryLock(syncLock.timeout(), TimeUnit.MILLISECONDS)) {
                throw new SyncLockTimeoutException("method: " + invocation.getMethod());
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
}
