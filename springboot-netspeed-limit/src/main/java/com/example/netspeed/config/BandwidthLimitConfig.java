package com.example.netspeed.config;

import com.example.netspeed.manager.BandwidthLimitManager;
import com.example.netspeed.web.BandwidthLimitInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 带宽限速配置类
 */
@Configuration
public class BandwidthLimitConfig implements WebMvcConfigurer {

    private BandwidthLimitInterceptor bandwidthLimitInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(bandwidthLimitInterceptor())
                .addPathPatterns("/api/**");
    }

    @Bean
    public BandwidthLimitInterceptor bandwidthLimitInterceptor() {
        if (bandwidthLimitInterceptor == null) {
            bandwidthLimitInterceptor = new BandwidthLimitInterceptor();
        }
        return bandwidthLimitInterceptor;
    }

    @Bean
    public BandwidthLimitManager bandwidthLimitManager() {
        return new BandwidthLimitManager();
    }
}
