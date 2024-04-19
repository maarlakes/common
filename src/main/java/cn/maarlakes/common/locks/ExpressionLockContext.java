package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;

/**
 * @author linjpxc
 */
public interface ExpressionLockContext extends LockContext {

    @Nonnull
    EvaluationContext evaluationContext();

    @Nonnull
    ExpressionParser expressionParser();

    @Nonnull
    String expression();

    @Nonnull
    @Override
    default String key() {
        try {
            Object root = this.evaluationContext().getRootObject().getValue();
            if (root == null) {
                root = this;
            }
            final Object value = this.expressionParser().parseExpression(this.expression()).getValue(this.evaluationContext(), root);
            if (value == null) {
                return this.expression();
            }
            return value.toString();
        } catch (Exception ignored) {
            return this.expression();
        }
    }

    @Nonnull
    static ExpressionLockContext create(@Nonnull EvaluationContext context, @Nonnull String expression) {
        return new DefaultExpressionLockContext(new SpelExpressionParser(), context, expression, false);
    }

    @Nonnull
    static ExpressionLockContext create(@Nonnull EvaluationContext context, @Nonnull String expression, boolean fair) {
        return new DefaultExpressionLockContext(new SpelExpressionParser(), context, expression, fair);
    }


    @Nonnull
    static ExpressionLockContext create(@Nonnull ExpressionParser parser, @Nonnull EvaluationContext context, @Nonnull String expression) {
        return new DefaultExpressionLockContext(parser, context, expression, false);
    }

    @Nonnull
    static ExpressionLockContext create(@Nonnull ExpressionParser parser, @Nonnull EvaluationContext context, @Nonnull String expression, boolean fair) {
        return new DefaultExpressionLockContext(parser, context, expression, fair);
    }
}
