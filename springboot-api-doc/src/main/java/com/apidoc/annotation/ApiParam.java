package com.apidoc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API参数注解
 * 用于增强参数描述信息
 */
@Target({ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiParam {

    /**
     * 参数名称
     */
    String name() default "";

    /**
     * 参数描述
     */
    String description() default "";

    /**
     * 是否必填
     */
    boolean required() default true;

    /**
     * 示例值
     */
    String example() default "";

    /**
     * 默认值
     */
    String defaultValue() default "";
}