package com.aihuishou.framework.lock.aop;

import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * default error handler
 * @author jiashuai.xie
 * @date Created in 2018/10/19 14:07
 */
public class DefaultErrorHandlerStrategy implements ErrorHandlerStrategy{

    @Override
    public Object handleError(Method targetMethod, Object[] args, Object result, ProceedingJoinPoint pjp, Throwable throwable) {
        throw new RuntimeException("default error handler handle error",throwable);
    }
}
