package com.example.utils;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EnvironmentUtil {
    
    private static final String DEFAULT_ENVIRONMENT = "development";
    
    /**
     * 获取当前环境
     */
    public String getCurrentEnvironment() {
        String env = System.getProperty("spring.profiles.active");
        if (env == null || env.trim().isEmpty()) {
            env = System.getenv("SPRING_PROFILES_ACTIVE");
        }
        if (env == null || env.trim().isEmpty()) {
            env = DEFAULT_ENVIRONMENT;
        }
        return env.trim();
    }
    
    /**
     * 判断是否为生产环境
     */
    public boolean isProduction() {
        return "production".equalsIgnoreCase(getCurrentEnvironment());
    }
    
    /**
     * 判断是否为开发环境
     */
    public boolean isDevelopment() {
        return "development".equalsIgnoreCase(getCurrentEnvironment());
    }
    
    /**
     * 判断是否为测试环境
     */
    public boolean isTest() {
        return "test".equalsIgnoreCase(getCurrentEnvironment());
    }
}