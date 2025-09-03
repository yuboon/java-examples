package com.example.hotpatch.annotation;

/**
 * 补丁类型枚举
 */
public enum PatchType {
    /**
     * Spring Bean 替换
     */
    SPRING_BEAN,
    
    /**
     * 普通Java类替换（整个类）
     */
    JAVA_CLASS,
    
    /**
     * 静态方法替换
     */
    STATIC_METHOD,
    
    /**
     * 实例方法替换
     */
    INSTANCE_METHOD
}