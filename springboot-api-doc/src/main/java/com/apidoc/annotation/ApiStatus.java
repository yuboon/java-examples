package com.apidoc.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * API状态注解
 * 用于标记API的开发状态
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ApiStatus {

    /**
     * API状态
     */
    Status value() default Status.STABLE;

    /**
     * 版本信息
     */
    String since() default "";

    /**
     * 废弃版本
     */
    String deprecatedSince() default "";

    /**
     * 状态说明
     */
    String description() default "";

    /**
     * API状态枚举
     */
    enum Status {
        /**
         * 开发中
         */
        DEVELOPMENT("开发中", "warning"),

        /**
         * 测试中
         */
        BETA("测试中", "info"),

        /**
         * 稳定版本
         */
        STABLE("稳定", "success"),

        /**
         * 已废弃
         */
        DEPRECATED("已废弃", "danger");

        private final String label;
        private final String cssClass;

        Status(String label, String cssClass) {
            this.label = label;
            this.cssClass = cssClass;
        }

        public String getLabel() {
            return label;
        }

        public String getCssClass() {
            return cssClass;
        }
    }
}