package com.apidoc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API操作注解
 * 用于描述API方法的基本信息
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiOperation {

    /**
     * API简短描述
     */
    String value();

    /**
     * API详细说明
     */
    String description() default "";

    /**
     * API备注信息
     */
    String notes() default "";

    /**
     * 是否隐藏此API
     */
    boolean hidden() default false;
}