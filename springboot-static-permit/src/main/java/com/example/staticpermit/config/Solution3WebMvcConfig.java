package com.example.staticpermit.config;

import com.example.staticpermit.interceptor.Solution3StaticResourceAuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 方案三：Web MVC配置，注册静态资源拦截器
 */
@Configuration
public class Solution3WebMvcConfig implements WebMvcConfigurer {

    @Autowired
    private Solution3StaticResourceAuthInterceptor staticResourceAuthInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 拦截所有对 /uploads/ 路径下资源的请求
        registry.addInterceptor(staticResourceAuthInterceptor)
                .addPathPatterns("/uploads/**");
    }
}