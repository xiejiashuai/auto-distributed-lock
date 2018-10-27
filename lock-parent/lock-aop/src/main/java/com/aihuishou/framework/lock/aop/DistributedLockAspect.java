package com.aihuishou.framework.lock.aop;

import com.aihuishou.framework.lock.aop.annotation.DistributedLock;
import com.aihuishou.framework.lock.aop.annotation.ErrorHandler;
import com.aihuishou.framework.lock.aop.exception.DistributedLockUnGettedException;
import com.aihuishou.framework.lock.aop.spel.LockEvaluationContext;
import com.aihuishou.framework.lock.aop.spel.LockOperationExpressionEvaluator;
import com.aihuishou.framework.lock.core.RedisLock;
import com.aihuishou.framework.lock.core.RedisLockManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Lock Aspect
 * when necessary , add lock before invoke target method
 *
 * @author jiashuai.xie
 */
@Aspect
public class DistributedLockAspect {

    private static final Logger LOGGER = LoggerFactory.getLogger(DistributedLockAspect.class);

    private final LockOperationExpressionEvaluator lockOperationExpressionEvaluator = new LockOperationExpressionEvaluator();

    @Pointcut("@annotation(com.aihuishou.framework.lock.aop.annotation.DistributedLock)")
    public void pointCut() {
    }

    private RedisLockManager lockManager;

    @Autowired(required = false)
    private List<DistributedLockPostProcessor> distributedLockPostProcessors;

    @Autowired(required = false)
    @ErrorHandler
    private ErrorHandlerStrategy errorHandlerStrategy = new DefaultErrorHandlerStrategy();

    @Around("pointCut()")
    public Object distributedLock(ProceedingJoinPoint pjp) {

        Object result = null;

        String lock = null;

        String distributedLockKey;

        Object[] args = pjp.getArgs();

        Method method = resolveMethod(pjp);

        RedisLock redisLock = null;

        try {

            DistributedLock distributedLockAnno = AnnotationUtils.getAnnotation(method, DistributedLock.class);

            String keyExpression = distributedLockAnno.key();

            String distributedLockKeyNameSpace = distributedLockAnno.lockNamespace();

            LockEvaluationContext context = lockOperationExpressionEvaluator.createEvaluationContext(method, args, pjp.getTarget().getClass());

            String keyValue = lockOperationExpressionEvaluator.key(keyExpression, context.getOperationMetadata().getMethodKey(), context, String.class);

            // the lock key
            distributedLockKey = distributedLockKeyNameSpace + ":" + keyValue;

            // lock key retry time
            int retryTime = distributedLockAnno.timeout();

            // lock expire time
            int expireTime = distributedLockAnno.expireTime();

            redisLock = lockManager.getLock(distributedLockKey, expireTime);

            boolean lockable = redisLock.tryLock(retryTime, TimeUnit.MILLISECONDS);

            LOGGER.info("get lock,lock is ----------->{}", lock);

            // failed to get lock
            if (!lockable) {
                LOGGER.error("failed to get lock,lockKey:{}", distributedLockKey);
                throw new DistributedLockUnGettedException("get lock failed");
            }

            result = pjp.proceed(args);


        } catch (Throwable e) {

            LOGGER.error("DistributedLockAspect invoke target method occur exception,msg:{}", e.getMessage(), e);

            errorHandlerStrategy.handleError(method, args, result, pjp, e);

        } finally {

            if (null != redisLock) {
                redisLock.unlock();
            }


        }

        // need to post processor
        if (!CollectionUtils.isEmpty(distributedLockPostProcessors)) {

            for (DistributedLockPostProcessor distributedLockPostProcessor : distributedLockPostProcessors) {
                distributedLockPostProcessor.postProcessAfterInvoking(method, args, result, pjp);
            }

        }

        return result;

    }

    /**
     * 获取要执行的方法
     *
     * @param joinPoint
     * @return
     */
    private Method resolveMethod(ProceedingJoinPoint joinPoint) {

        MethodSignature signature = (MethodSignature) joinPoint.getSignature();

        Class<?> targetClass = joinPoint.getTarget().getClass();

        Method method = getDeclaredMethodFor(targetClass, signature.getName(), signature.getMethod().getParameterTypes());
        if (method == null) {
            throw new IllegalStateException("Cannot resolve target method: " + signature.getMethod().getName());
        }
        return method;
    }

    private Method getDeclaredMethodFor(Class<?> clazz, String name, Class<?>... parameterTypes) {
        try {
            return clazz.getDeclaredMethod(name, parameterTypes);
        } catch (NoSuchMethodException e) {
            Class<?> superClass = clazz.getSuperclass();
            if (superClass != null) {
                return getDeclaredMethodFor(superClass, name, parameterTypes);
            }
        }
        return null;
    }

    public void setLockManager(RedisLockManager lockManager) {
        this.lockManager = lockManager;
    }
}
