package com.example.version;

import java.lang.annotation.*;

/**
 * 灰度发布注解，用于定义灰度发布规则
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface GrayRelease {
    /**
     * 开始时间，格式：yyyy-MM-dd HH:mm:ss
     */
    String startTime() default "";
    
    /**
     * 结束时间，格式：yyyy-MM-dd HH:mm:ss
     */
    String endTime() default "";
    
    /**
     * 用户ID白名单，多个ID用逗号分隔
     */
    String userIds() default "";
    
    /**
     * 用户比例，0-100之间的整数，表示百分比
     */
    int percentage() default 0;
    
    /**
     * 指定的用户组
     */
    String[] userGroups() default {};
    
    /**
     * 地区限制，支持国家、省份、城市，如：CN,US,Beijing
     */
    String[] regions() default {};
}