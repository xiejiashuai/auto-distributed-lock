package com.aihuishou.springframework.boot.lock.autoconfigure.annotation;

import com.aihuishou.springframework.boot.lock.autoconfigure.Mark;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * @author jiashuai.xie
 * @date Created in 2018/10/24 17:50
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@Import(Mark.class)
public @interface EnableAutoLocked {
}
