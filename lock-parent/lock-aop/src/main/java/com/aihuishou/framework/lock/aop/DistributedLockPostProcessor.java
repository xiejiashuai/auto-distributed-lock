package com.aihuishou.framework.lock.aop;

import org.aspectj.lang.ProceedingJoinPoint;

import java.lang.reflect.Method;

/**
 * do something after <strong>success </strong> to get lock and <strong>success </strong> to invoke target method
 *
 * @author jiashuai.xie
 */
public interface DistributedLockPostProcessor {

    /**
     *
     * @param targetMethod target method
     * @param args  target method  args
     * @param result the value that target method returned
     * @param pjp if want to get other info ,can use
     * @return

     */
    default Object postProcessAfterInvoking(Method targetMethod,Object[] args,Object result,ProceedingJoinPoint pjp) {
        return null;
    }


}
