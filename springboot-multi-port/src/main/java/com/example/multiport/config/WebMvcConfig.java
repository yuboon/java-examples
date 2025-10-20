package com.example.multiport.config;

import com.example.multiport.annotation.AdminApi;
import com.example.multiport.annotation.UserApi;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 为不同API类型配置路径前缀
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // 为用户端Controller添加/api/user前缀
        configurer.addPathPrefix("/api/user",
            cls -> cls.isAnnotationPresent(UserApi.class));

        // 为管理端Controller添加/api/admin前缀
        configurer.addPathPrefix("/api/admin",
            cls -> cls.isAnnotationPresent(AdminApi.class));
    }
}