package cn.maarlakes.common.locks;

import jakarta.annotation.Nonnull;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.ExpressionParser;

/**
 * @author linjpxc
 */
class DefaultExpressionLockContext implements ExpressionLockContext {

    private final ExpressionParser parser;
    private final EvaluationContext context;
    private final String expression;
    private final boolean fair;

    public DefaultExpressionLockContext(@Nonnull ExpressionParser parser, @Nonnull EvaluationContext context, @Nonnull String expression, boolean fair) {
        this.parser = parser;
        this.context = context;
        this.expression = expression;
        this.fair = fair;
    }

    @Nonnull
    @Override
    public EvaluationContext evaluationContext() {
        return this.context;
    }

    @Nonnull
    @Override
    public ExpressionParser expressionParser() {
        return this.parser;
    }

    @Nonnull
    @Override
    public String expression() {
        return this.expression;
    }

    @Override
    public boolean isFair() {
        return this.fair;
    }

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o instanceof ExpressionLockContext) {
            final ExpressionLockContext that = (ExpressionLockContext) o;
            return fair == that.isFair() && parser.equals(that.expressionParser()) && context.equals(that.evaluationContext()) && expression.equals(that.expression());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = parser.hashCode();
        result = 31 * result + context.hashCode();
        result = 31 * result + expression.hashCode();
        result = 31 * result + Boolean.hashCode(fair);
        return result;
    }

    @Override
    public String toString() {
        return "expression='" + expression + '\'' + ", fair=" + fair;
    }
}
