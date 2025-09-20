package com.apidoc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API示例注解
 * 用于指定示例数据类和场景
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiExample {

    /**
     * 示例数据类
     */
    Class<?> value();

    /**
     * 场景名称
     * 如: success, error, empty等
     */
    String scenario() default "default";

    /**
     * 是否生成真实数据
     * true: 基于字段名生成合理数据
     * false: 生成简单默认值
     */
    boolean realistic() default true;

    /**
     * 示例描述
     */
    String description() default "";
}