package com.example.version;

import java.lang.annotation.*;

/**
 * API版本注解，用于标记接口的版本
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ApiVersion {
    /**
     * 版本号，默认为1.0
     */
    String value() default "1.0";
    
    /**
     * 版本描述
     */
    String description() default "";
    
    /**
     * 是否废弃
     */
    boolean deprecated() default false;
    
    /**
     * 废弃说明，建议使用的新版本等信息
     */
    String deprecatedDesc() default "";
}