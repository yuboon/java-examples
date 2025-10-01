package com.license.annotation;

import java.lang.annotation.*;

/**
 * 功能权限注解
 * 用于标记需要特定功能权限的方法或类
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireFeature {

    /**
     * 需要的功能权限
     */
    String value();

    /**
     * 权限不足时的提示信息
     */
    String message() default "功能未授权";
}
