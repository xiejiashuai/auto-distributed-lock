package com.aihuishou.framework.lock.aop.spel;

import org.springframework.context.expression.AnnotatedElementKey;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.ParameterNameDiscoverer;

import java.lang.reflect.Method;

/**
 * @author jiashuai.xie
 */
public class LockEvaluationContext extends MethodBasedEvaluationContext {

    private LockOperationMetadata operationMetadata;

    public LockEvaluationContext(Method targetMethod, Object[] arguments, ParameterNameDiscoverer parameterNameDiscoverer) {
        super(null, targetMethod, arguments, parameterNameDiscoverer);
    }

    public static class LockOperationMetadata {

        private final Method method;

        private final Class<?> targetClass;

        private final Method targetMethod;

        private final AnnotatedElementKey methodKey;


        public LockOperationMetadata(Method method, Class<?> targetClass, Method targetMethod) {
            this.method = method;
            this.targetClass = targetClass;
            this.targetMethod = targetMethod;
            this.methodKey = new AnnotatedElementKey(targetMethod, targetClass);
        }

        public Method getMethod() {
            return method;
        }

        public Class<?> getTargetClass() {
            return targetClass;
        }

        public Method getTargetMethod() {
            return targetMethod;
        }

        public AnnotatedElementKey getMethodKey() {
            return methodKey;
        }
    }

    public LockOperationMetadata getOperationMetadata() {
        return operationMetadata;
    }

    public void setOperationMetadata(LockOperationMetadata operationMetadata) {
        this.operationMetadata = operationMetadata;
    }
}
