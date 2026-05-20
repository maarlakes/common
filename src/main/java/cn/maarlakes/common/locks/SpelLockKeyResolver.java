package cn.maarlakes.common.locks;

import cn.maarlakes.common.factory.bean.BeanFactories;
import jakarta.annotation.Nonnull;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.core.StandardReflectionParameterNameDiscoverer;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import java.lang.reflect.Method;

/**
 * SpEL 表达式锁 key 解析工具。
 *
 * @author linjpxc
 */
public final class SpelLockKeyResolver {

    private static final Logger log = LoggerFactory.getLogger(SpelLockKeyResolver.class);

    private static final ExpressionParser PARSER = new SpelExpressionParser();
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new StandardReflectionParameterNameDiscoverer();

    private SpelLockKeyResolver() {
    }

    /**
     * 解析锁 key 表达式。如果表达式为空则使用默认 key。
     *
     * @param expression 锁 key 表达式（可能是 SpEL 表达式或普通字符串）
     * @param invocation  方法调用上下文
     * @return 解析后的 key
     * @throws LockException 如果 SpEL 表达式解析失败
     */
    @Nonnull
    public static String resolveKey(@Nonnull String expression, @Nonnull MethodInvocation invocation) {
        if (expression.isEmpty()) {
            final String key = defaultKey(invocation.getMethod());
            if (log.isDebugEnabled()) {
                log.debug("未指定锁 key 表达式，使用默认值：{}", key);
            }
            return key;
        }

        final StandardEvaluationContext context = new StandardEvaluationContext(invocation.getThis());
        final String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(invocation.getMethod());
        if (parameterNames != null) {
            final Object[] args = invocation.getArguments();
            for (int i = 0; i < parameterNames.length; i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }
        context.setBeanResolver((c, beanName) -> BeanFactories.getBean(beanName));

        return evaluateKey(expression, context);
    }

    @Nonnull
    private static String evaluateKey(@Nonnull String expression, @Nonnull EvaluationContext context) {
        try {
            final Object root = context.getRootObject().getValue();
            final Object value = PARSER.parseExpression(expression).getValue(context, root != null ? root : new Object());
            if (value == null) {
                log.warn("锁 key 表达式求值为 null：{}", expression);
                throw new LockException("Lock key expression evaluated to null: " + expression);
            }
            final String key = value.toString();
            if (log.isDebugEnabled()) {
                log.debug("解析锁 key 表达式 '{}' -> '{}'", expression, key);
            }
            return key;
        } catch (LockException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析锁 key 表达式失败：{}", expression, e);
            throw new LockException("Failed to resolve lock key expression: " + expression, e);
        }
    }

    @Nonnull
    static String defaultKey(@Nonnull Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }
}
