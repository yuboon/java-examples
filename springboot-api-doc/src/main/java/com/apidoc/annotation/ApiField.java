package com.apidoc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段描述注解
 * 用于描述实体类字段的中文信息
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiField {

    /**
     * 字段描述
     */
    String value() default "";

    /**
     * 字段名称（如果与字段名不同）
     */
    String name() default "";

    /**
     * 是否必填
     */
    boolean required() default false;

    /**
     * 示例值
     */
    String example() default "";

    /**
     * 字段是否隐藏
     */
    boolean hidden() default false;
}