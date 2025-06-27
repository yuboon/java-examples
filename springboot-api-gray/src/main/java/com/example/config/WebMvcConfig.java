package com.example.config;

import com.example.version.ApiVersionRequestMappingHandlerMapping;
import org.springframework.boot.autoconfigure.web.servlet.WebMvcRegistrations;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer, WebMvcRegistrations {
    
    @Override
    public RequestMappingHandlerMapping getRequestMappingHandlerMapping() {
        return new ApiVersionRequestMappingHandlerMapping();
    }
    
    /*@Bean
    public GrayReleaseInterceptor grayReleaseInterceptor() {
        return new GrayReleaseInterceptor();
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(grayReleaseInterceptor())
                .addPathPatterns("/api/**")
                .order(Ordered.HIGHEST_PRECEDENCE); // 最高优先级
    }*/
}