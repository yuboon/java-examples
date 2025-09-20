package com.apidoc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API环境注解
 * 用于标记API在哪些环境下可见
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiEnvironment {

    /**
     * 环境列表
     * 如: development, test, production, all
     */
    String[] value() default {"all"};

    /**
     * 环境说明
     */
    String description() default "";
}