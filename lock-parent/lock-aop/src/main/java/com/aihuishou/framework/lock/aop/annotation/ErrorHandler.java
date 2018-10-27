package com.aihuishou.framework.lock.aop.annotation;

import org.springframework.beans.factory.annotation.Qualifier;

import java.lang.annotation.*;

/**
 * @author jiashuai.xie
 * @date Created in 2018/10/19 14:22
 */
@Target({ElementType.FIELD,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface ErrorHandler {
}
