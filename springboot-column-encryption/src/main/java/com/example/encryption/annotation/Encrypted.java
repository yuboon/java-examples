package com.example.encryption.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 字段级加密注解
 * 标记需要进行加解密的字段
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Encrypted {
    /**
     * 加密算法类型，默认为 AES-GCM
     */
    Algorithm algorithm() default Algorithm.AES_GCM;

    /**
     * 是否支持模糊查询
     */
    boolean searchable() default false;

    /**
     * 支持的加密算法枚举
     */
    enum Algorithm {
        AES_GCM
    }
}