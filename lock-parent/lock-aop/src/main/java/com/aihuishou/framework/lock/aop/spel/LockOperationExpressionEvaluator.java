package com.aihuishou.framework.lock.aop.spel;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.core.BridgeMethodResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility class handling the SpEL expression parsing.
 * Meant to be used as a reusable, thread-safe componen
 *
 * @author jiashuai.xie
 */
public class LockOperationExpressionEvaluator extends LockedExpressionEvaluator {

    private final Map<ExpressionKey, Expression> keyCache = new ConcurrentHashMap<>(64);


    /**
     * Create an {@link EvaluationContext}.
     *
     * @param method the actual method
     * @param args   the method arguments
     * @return the evaluation context
     */
    public LockEvaluationContext createEvaluationContext(Method method, Object[] args, Class<?> clazz) {

        method = BridgeMethodResolver.findBridgedMethod(method);

        // get actual target clazz
        if (AopUtils.isAopProxy(clazz)) {
            clazz = AopUtils.getTargetClass(clazz);
        }

        Method targetMethod = (!Proxy.isProxyClass(clazz) ? AopUtils.getMostSpecificMethod(method, clazz) : method);

        LockEvaluationContext.LockOperationMetadata operationMetadata = new LockEvaluationContext.LockOperationMetadata(method, clazz, targetMethod);

        LockEvaluationContext evaluationContext = new LockEvaluationContext(targetMethod, args, getParameterNameDiscoverer());

        evaluationContext.setOperationMetadata(operationMetadata);

        return evaluationContext;

    }

    public <T> T key(String keyExpression, AnnotatedElementKey methodKey, EvaluationContext evalContext, Class<T> targetType) {
        return getExpression(this.keyCache, methodKey, keyExpression).getValue(evalContext, targetType);
    }


}
