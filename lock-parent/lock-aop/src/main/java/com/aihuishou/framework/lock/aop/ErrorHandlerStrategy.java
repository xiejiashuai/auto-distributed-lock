package com.aihuishou.framework.lock.aop;

import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * do something after invoking target method occur exception
 * @author jiashuai.xie
 * @date Created in 2018/10/19 14:07
 */
public interface ErrorHandlerStrategy {

    /**
     *
     * @param targetMethod target method
     * @param args  target method  args
     * @param result the value that target method returned
     * @param pjp if want to get other info ,can use
     * @param throwable the exception when invoking target method
     * @return
     */
    default Object handleError(Method targetMethod, Object[] args, Object result, ProceedingJoinPoint pjp,Throwable throwable) {
        return null;
    }

}
