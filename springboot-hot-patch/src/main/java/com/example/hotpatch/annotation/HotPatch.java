package com.example.hotpatch.annotation;

import java.lang.annotation.*;

/**
 * 增强的热补丁注解
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface HotPatch {
    /**
     * 补丁类型
     */
    PatchType type() default PatchType.SPRING_BEAN;
    
    /**
     * 原始Bean名称（当type=SPRING_BEAN时使用）
     */
    String originalBean() default "";
    
    /**
     * 原始类的全限定名（当type=JAVA_CLASS或STATIC_METHOD时使用）
     */
    String originalClass() default "";
    
    /**
     * 要替换的方法名（当type=STATIC_METHOD或INSTANCE_METHOD时使用）
     */
    String methodName() default "";
    
    /**
     * 方法签名（用于方法重载区分）
     */
    String methodSignature() default "";
    
    /**
     * 补丁版本
     */
    String version() default "1.0";
    
    /**
     * 补丁描述
     */
    String description() default "";
    
    /**
     * 是否启用安全验证
     */
    boolean securityCheck() default true;
}