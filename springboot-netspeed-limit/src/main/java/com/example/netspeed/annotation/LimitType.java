package com.example.netspeed.annotation;

/**
 * 限速类型枚举
 */
public enum LimitType {
    /**
     * 全局限速 - 所有请求共享限速桶
     */
    GLOBAL,
    /**
     * 按接口限速 - 每个接口独立限速
     */
    API,
    /**
     * 按用户限速 - 根据用户标识（如用户ID、token）限速
     */
    USER,
    /**
     * 按IP限速 - 根据请求IP限速
     */
    IP
}
