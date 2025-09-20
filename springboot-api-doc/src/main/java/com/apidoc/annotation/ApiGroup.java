package com.apidoc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API分组注解
 * 用于对API进行分组管理
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiGroup {

    /**
     * 分组名称
     */
    String name();

    /**
     * 分组描述
     */
    String description() default "";

    /**
     * 排序权重，数字越小越靠前
     */
    int order() default 0;

    /**
     * API版本
     */
    String version() default "v1";

    /**
     * 分组标签
     */
    String[] tags() default {};
}