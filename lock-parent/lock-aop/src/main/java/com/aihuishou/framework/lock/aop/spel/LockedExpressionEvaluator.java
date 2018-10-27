package com.aihuishou.framework.lock.aop.spel;

import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

import java.util.Map;

/**
 * Shared utility class used to evaluate and cache SpEL expressions that
 * are defined on {@link java.lang.reflect.AnnotatedElement}.
 *
 * @author jiashuai.xie
 * @see AnnotatedElementKey
 */
public abstract class LockedExpressionEvaluator {

    private final SpelExpressionParser parser;

    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();


    /**
     * Create a new instance with the specified {@link SpelExpressionParser}.
     */
    protected LockedExpressionEvaluator(SpelExpressionParser parser) {
        Assert.notNull(parser, "SpelExpressionParser must not be null");
        this.parser = parser;
    }

    /**
     * Create a new instance with a default {@link SpelExpressionParser}.
     */
    protected LockedExpressionEvaluator() {
        this(new SpelExpressionParser());
    }


    /**
     * Return the {@link SpelExpressionParser} to use.
     */
    protected SpelExpressionParser getParser() {
        return this.parser;
    }

    /**
     * Return a shared parameter name discoverer which caches data internally.
     *
     * @since 4.3
     */
    protected ParameterNameDiscoverer getParameterNameDiscoverer() {
        return this.parameterNameDiscoverer;
    }


    /**
     * Return the {@link Expression} for the specified SpEL value
     * <p>Parse the expression if it hasn't been already.
     *
     * @param cache      the cache to use
     * @param elementKey the element on which the expression is defined
     * @param expression the expression to parse
     */
    protected Expression getExpression(Map<ExpressionKey, Expression> cache,
                                       AnnotatedElementKey elementKey, String expression) {

        LockedExpressionEvaluator.ExpressionKey expressionKey = createKey(elementKey, expression);
        Expression expr = cache.get(expressionKey);
        if (expr == null) {
            expr = getParser().parseExpression(expression);
            cache.put(expressionKey, expr);
        }
        return expr;
    }

    private LockedExpressionEvaluator.ExpressionKey createKey(AnnotatedElementKey elementKey, String expression) {
        return new LockedExpressionEvaluator.ExpressionKey(elementKey, expression);
    }


    protected static class ExpressionKey implements Comparable<ExpressionKey> {

        private final AnnotatedElementKey element;

        private final String expression;

        protected ExpressionKey(AnnotatedElementKey element, String expression) {
            Assert.notNull(element, "AnnotatedElementKey must not be null");
            Assert.notNull(expression, "Expression must not be null");
            this.element = element;
            this.expression = expression;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof LockedExpressionEvaluator.ExpressionKey)) {
                return false;
            }
            LockedExpressionEvaluator.ExpressionKey otherKey = (LockedExpressionEvaluator.ExpressionKey) other;
            return (this.element.equals(otherKey.element) &&
                    ObjectUtils.nullSafeEquals(this.expression, otherKey.expression));
        }

        @Override
        public int hashCode() {
            return this.element.hashCode() * 29 + this.expression.hashCode();
        }

        @Override
        public String toString() {
            return this.element + " with expression \"" + this.expression + "\"";
        }

        @Override
        public int compareTo(LockedExpressionEvaluator.ExpressionKey other) {
            int result = this.element.toString().compareTo(other.element.toString());
            if (result == 0) {
                result = this.expression.compareTo(other.expression);
            }
            return result;
        }
    }

}
