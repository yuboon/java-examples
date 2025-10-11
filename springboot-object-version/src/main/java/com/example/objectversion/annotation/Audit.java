package com.example.objectversion.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 审计注解，用于标记需要进行变更审计的方法
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Audit {

    /**
     * ID字段名，用于从实体中提取ID
     */
    String idField() default "id";

    /**
     * ID参数名，如果指定则直接从方法参数中获取ID
     */
    String idParam() default "";

    /**
     * 操作类型，如果未指定则根据方法名自动推断
     */
    ActionType action() default ActionType.AUTO;

    /**
     * 操作人参数名
     */
    String actorParam() default "";

    /**
     * 实体参数位置
     */
    int entityIndex() default 0;

    /**
     * 操作类型枚举
     */
    enum ActionType {
        CREATE, UPDATE, DELETE, AUTO
    }
}