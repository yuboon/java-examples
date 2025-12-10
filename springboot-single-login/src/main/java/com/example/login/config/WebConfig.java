package com.example.login.config;

import com.example.login.interceptor.LoginInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web配置类
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginInterceptor loginInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(loginInterceptor)
                .addPathPatterns("/**")  // 拦截所有请求
                .excludePathPatterns(
                    "/",                    // 首页
                    "/login.html",          // 登录页面
                    "/index.html",          // 主页
                    "/admin.html",          // 管理页面
                    "/error",               // 错误页面
                    "/favicon.ico",         // 图标
                    "/css/**",              // CSS文件
                    "/js/**",               // JS文件
                    "/images/**",           // 图片文件
                    "/api/auth/login",      // 登录API
                    "/api/auth/register",   // 注册API（如果有）
                    "/api/status"           // 状态检查API
                );
    }
}