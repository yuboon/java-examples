package com.example.login.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 登录配置类
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.login")
public class LoginProperties {

    /**
     * 登录模式：SINGLE-单用户单登录，MULTIPLE-单用户多登录
     */
    private LoginMode mode = LoginMode.SINGLE;

    /**
     * Token有效期（秒）
     */
    private long tokenExpireTime = 30 * 60;

    /**
     * Token前缀
     */
    private String tokenPrefix = "TOKEN_";

    /**
     * Token请求头名称
     */
    private String tokenHeader = "Authorization";

    /**
     * 是否启用自动清理过期Token
     */
    private boolean enableAutoClean = true;

    /**
     * 清理间隔（分钟）
     */
    private int cleanInterval = 5;

    /**
     * 登录模式枚举
     */
    public enum LoginMode {
        // 单用户单登录（新登录踢出旧登录）
        SINGLE,
        // 单用户多登录（允许多个设备同时登录）
        MULTIPLE
    }
}