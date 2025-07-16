package com.example.rest.config;

import com.example.rest.interceptor.AuthInterceptor;
import com.example.rest.interceptor.LoggingInterceptor;
import com.example.rest.interceptor.RetryInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class RestTemplateConfig {

    @Bean
    public RestTemplate customRestTemplate() {
        // 1. 创建拦截器列表（顺序很重要！）
        List<ClientHttpRequestInterceptor> interceptors = new ArrayList<>();
        interceptors.add(new AuthInterceptor());       // 1. 认证拦截器（先加请求头）
        interceptors.add(new LoggingInterceptor());    // 2. 日志拦截器（记录完整请求）
        interceptors.add(new RetryInterceptor(2));     // 3. 重试拦截器（最后处理失败）

        // 2. 创建RestTemplate并设置拦截器
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setInterceptors(interceptors);

        // 3. 解决响应流只能读取一次的问题（关键配置！）
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000);  // 连接超时3秒
        factory.setReadTimeout(3000);     // 读取超时3秒
        restTemplate.setRequestFactory(new BufferingClientHttpRequestFactory(factory));

        return restTemplate;
    }
}