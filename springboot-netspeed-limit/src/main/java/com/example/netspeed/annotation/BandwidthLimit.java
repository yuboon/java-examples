package com.example.netspeed.annotation;

import com.example.netspeed.annotation.BandwidthUnit;
import com.example.netspeed.annotation.LimitType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 带宽限速注解
 *
 * 支持多种限速维度：
 * - GLOBAL: 全局限速，所有请求共享限速桶
 * - API: 按接口限速，每个接口独立限速
 * - USER: 按用户限速，根据用户标识（如请求头 X-User-Id）限速
 * - IP: 按IP限速，根据请求IP限速
 *
 * 使用示例：
 * <pre>
 * // 限速 200 KB/s
 * {@code @BandwidthLimit(value = 200, unit = BandwidthUnit.KB)}
 *
 * // 按用户限速，免费用户 200KB/s，VIP用户 1MB/s
 * {@code @BandwidthLimit(value = 200, unit = BandwidthUnit.KB, type = LimitType.USER)}
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface BandwidthLimit {

    /**
     * 限速值
     */
    long value() default 200;

    /**
     * 限速单位
     */
    BandwidthUnit unit() default BandwidthUnit.KB;

    /**
     * 限速类型
     */
    LimitType type() default LimitType.GLOBAL;

    /**
     * 免费用户限速值（-1表示不区分）
     */
    long free() default -1;

    /**
     * VIP用户限速值（-1表示不区分）
     */
    long vip() default -1;

    /**
     * 桶容量倍数（相对于填充速率）
     * 1.0 表示桶容量 = 1秒流量
     * 0.5 表示桶容量 = 0.5秒流量（更平滑）
     * 2.0 表示桶容量 = 2秒流量（允许更大突发）
     */
    double capacityMultiplier() default 1.0;

    /**
     * 分块大小（字节），-1 表示自动计算
     */
    int chunkSize() default -1;

    /**
     * 用户标识请求头名称（用于 USER 类型限速）
     */
    String userHeader() default "X-User-Id";
}
