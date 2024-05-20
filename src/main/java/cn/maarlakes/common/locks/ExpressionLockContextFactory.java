package cn.maarlakes.common.locks;

import cn.maarlakes.common.factory.bean.BeanFactories;
import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.expression.spel.support.StandardEvaluationContext;

/**
 * @author linjpxc
 */
public class ExpressionLockContextFactory implements LockContextFactory {

    private final ParameterNameDiscoverer parameterNameDiscoverer = new StandardReflectionParameterNameDiscoverer();

    @Override
    public LockContext create(@Nonnull String key, @Nonnull MethodInvocation invocation) {
        final StandardEvaluationContext context = new StandardEvaluationContext(invocation.getThis());
        final String[] parameterNames = this.parameterNameDiscoverer.getParameterNames(invocation.getMethod());
        if (parameterNames != null) {
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], invocation.getArguments()[i]);
            }
        }
        context.setBeanResolver((c, beanName) -> BeanFactories.getBean(beanName));
        final SyncLock syncLock = AnnotatedElementUtils.findMergedAnnotation(invocation.getMethod(), SyncLock.class);
        return ExpressionLockContext.create(context, key, syncLock != null && syncLock.fair());
    }
}
