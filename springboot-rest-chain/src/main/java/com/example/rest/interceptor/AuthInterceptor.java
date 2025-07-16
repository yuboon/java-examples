package com.example.rest.interceptor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

public class AuthInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // 1. 从缓存获取Token（实际项目可能从Redis或配置中心获取）
        String token = getToken();
        
        // 2. 添加Authorization请求头
        HttpHeaders headers = request.getHeaders();
        headers.set("Authorization", "Bearer " + token);  // Bearer认证格式
        
        // 3. 传递给下一个拦截器
        return execution.execute(request, body);
    }

    // 模拟从缓存获取Token
    private String getToken() {
        return "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...";  // 实际项目是真实Token
    }
}