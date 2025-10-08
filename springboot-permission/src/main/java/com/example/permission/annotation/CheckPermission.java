package com.example.permission.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 权限检查注解
 * 用于方法级别的权限控制
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CheckPermission {

    /**
     * 操作类型：read, edit, delete 等
     */
    String action();

    /**
     * 资源参数名称（默认从方法参数中获取第一个 Document 类型）
     */
    String resourceParam() default "";
}
