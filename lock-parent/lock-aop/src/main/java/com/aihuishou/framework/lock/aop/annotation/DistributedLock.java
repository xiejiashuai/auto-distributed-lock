package com.aihuishou.framework.lock.aop.annotation;

import java.lang.annotation.*;

/**
 * Annotation indicating that a method (or all methods)  need add lock
 *
 * @author jiashuai.xie
 */

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface DistributedLock {

    /**
     * namespace of the lock to use
     */
    String lockNamespace() default "";

    /**
     * Spring Expression Language (SpEL) expression for computing the key dynamically.
     */
    String key();

    /**
     * when failed to get lock and timeout > 0 ,will retry get lock
     * use default value means that when get lock failed,will fail fast
     */
    int timeout() default 0;

    /**
     * the lock expire time
     */
    int expireTime();

}
