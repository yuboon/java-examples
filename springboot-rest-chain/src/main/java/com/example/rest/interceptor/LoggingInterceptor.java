package com.example.rest.interceptor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
public class LoggingInterceptor implements ClientHttpRequestInterceptor {
    @Override
    public ClientHttpResponse intercept(HttpRequest request, byte[] body, ClientHttpRequestExecution execution) throws IOException {
        // 1. 记录请求信息
        long start = System.currentTimeMillis();
        log.info("===== 请求开始 =====");
        log.info("URL: {}", request.getURI());
        log.info("Method: {}", request.getMethod());
        log.info("Headers: {}", request.getHeaders());
        log.info("Body: {}", new String(body, StandardCharsets.UTF_8));

        // 2. 执行下一个拦截器（关键！不调用会中断链条）
        ClientHttpResponse response = execution.execute(request, body);

        // 3. 记录响应信息
        String responseBody = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
        log.info("===== 请求结束 =====");
        log.info("Status: {}", response.getStatusCode());
        log.info("Headers: {}", response.getHeaders());
        log.info("Body: {}", responseBody);
        log.info("耗时: {}ms", System.currentTimeMillis() - start);

        return response;
    }
}