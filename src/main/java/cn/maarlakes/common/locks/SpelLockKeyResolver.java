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
 * <p>将 {@link SyncLock#value()} 中的表达式解析为最终的锁 key 字符串。</p>
 *
 * <h3>SpEL 上下文设置</h3>
 * <ul>
 *     <li><b>根对象</b>：目标方法的调用对象（{@code invocation.getThis()}），
 *         可在表达式中通过 {@code #root} 引用</li>
 *     <li><b>变量</b>：方法参数，通过 {@link StandardReflectionParameterNameDiscoverer}
 *         发现参数名，以 {@code #参数名} 形式引用，如 {@code #userId}、{@code #order.id}</li>
 *     <li><b>Bean 解析器</b>：支持 {@code @beanName} 语法引用 Spring Bean，
 *         通过 {@link BeanFactories#getBean(String)} 解析</li>
 * </ul>
 *
 * <h3>线程安全性</h3>
 * <p>共享的 {@link SpelExpressionParser} 实例是线程安全的。
 * 每次调用 {@link #resolveKey} 会创建独立的 {@link StandardEvaluationContext}，无并发问题。</p>
 *
 * @author linjpxc
 * @see SyncLock
 * @see SyncLockContextResolver
 */
public final class SpelLockKeyResolver {

    private static final Logger log = LoggerFactory.getLogger(SpelLockKeyResolver.class);

    private static final ExpressionParser PARSER = new SpelExpressionParser();

    // 用于发现方法参数名，不依赖 -parameters 编译选项
    private static final ParameterNameDiscoverer PARAMETER_NAME_DISCOVERER = new StandardReflectionParameterNameDiscoverer();

    private SpelLockKeyResolver() {
    }

    /**
     * 解析锁 key 表达式。
     *
     * <p>行为如下：</p>
     * <ul>
     *     <li>表达式为空 — 使用默认 key（{@code 类全限定名.方法名}）</li>
     *     <li>表达式非空 — 作为 SpEL 表达式解析，将方法参数绑定到上下文变量</li>
     * </ul>
     *
     * @param expression 锁 key 表达式（可能是 SpEL 表达式或空字符串）
     * @param invocation  方法调用上下文
     * @return 解析后的 key 字符串
     * @throws LockException 如果 SpEL 表达式求值为 null 或解析失败
     */
    @Nonnull
    public static String resolveKey(@Nonnull String expression, @Nonnull MethodInvocation invocation) {
        if (expression.isEmpty()) {
            final String key = defaultKey(invocation.getMethod());
            if (log.isTraceEnabled()) {
                log.trace("未指定锁 key 表达式，使用默认值：{}", key);
            }
            return key;
        }

        final StandardEvaluationContext context = new StandardEvaluationContext(invocation.getThis());
        final String[] parameterNames = PARAMETER_NAME_DISCOVERER.getParameterNames(invocation.getMethod());
        if (parameterNames != null) {
            if (log.isTraceEnabled()) {
                log.trace("发现方法 {} 的参数名：{}", invocation.getMethod().getName(), String.join(", ", parameterNames));
            }
            final Object[] args = invocation.getArguments();
            for (int i = 0; i < Math.min(parameterNames.length, args.length); i++) {
                context.setVariable(parameterNames[i], args[i]);
            }
        }

        // 注册 Bean 解析器，支持在 SpEL 中通过 @beanName 引用 Spring Bean
        context.setBeanResolver((c, beanName) -> BeanFactories.getBean(beanName));

        return evaluateKey(expression, context);
    }

    /**
     * 执行 SpEL 表达式求值。
     *
     * <p>如果根对象为 null，使用 {@code new Object()} 替代，避免 SpEL 解析异常。</p>
     *
     * @param expression SpEL 表达式
     * @param context    求值上下文
     * @return 求值结果的字符串表示
     * @throws LockException 如果表达式求值为 null 或解析失败
     */
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
            if (log.isTraceEnabled()) {
                log.trace("解析锁 key 表达式 '{}' -> '{}'", expression, key);
            }
            return key;
        } catch (LockException e) {
            throw e;
        } catch (Exception e) {
            log.warn("解析锁 key 表达式失败：{}", expression, e);
            throw new LockException("Failed to resolve lock key expression: " + expression, e);
        }
    }

    /**
     * 生成默认锁 key：{@code 类全限定名.方法名}。
     */
    @Nonnull
    static String defaultKey(@Nonnull Method method) {
        return method.getDeclaringClass().getName() + "." + method.getName();
    }
}
