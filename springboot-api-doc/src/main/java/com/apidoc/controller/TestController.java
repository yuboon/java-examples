package com.apidoc.controller;

import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * API测试控制器
 * 提供在线API测试功能
 */
@RestController
@RequestMapping("/api-doc/test")
public class TestController {

    private final WebClient webClient;

    public TestController() {
        this.webClient = WebClient.builder()
            .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(1024 * 1024)) // 1MB
            .build();
    }

    /**
     * 执行API测试
     */
    @PostMapping("/execute")
    public Mono<Map<String, Object>> executeTest(@RequestBody TestRequest request) {
        long startTime = System.currentTimeMillis();

        try {
            // 构建请求
            WebClient.RequestHeadersUriSpec<?> requestSpec = webClient
                .method(org.springframework.http.HttpMethod.valueOf(request.getMethod().toUpperCase()));

            // 设置URI
            WebClient.RequestHeadersSpec<?> headersSpec = requestSpec.uri(request.getUrl());

            // 添加Headers
            if (request.getHeaders() != null) {
                for (Map.Entry<String, String> header : request.getHeaders().entrySet()) {
                    headersSpec = headersSpec.header(header.getKey(), header.getValue());
                }
            }

            // 添加请求体
            WebClient.ResponseSpec responseSpec;
            if (request.getBody() != null && !request.getBody().trim().isEmpty()) {
                responseSpec = ((WebClient.RequestBodySpec) headersSpec)
                    .bodyValue(request.getBody())
                    .retrieve();
            } else {
                responseSpec = headersSpec.retrieve();
            }

            // 执行请求并处理响应
            return responseSpec
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(30))
                .map(responseBody -> {
                    long endTime = System.currentTimeMillis();
                    return buildSuccessResponse(responseBody, endTime - startTime, 200, "OK");
                })
                .onErrorResume(error -> {
                    long endTime = System.currentTimeMillis();
                    return Mono.just(buildErrorResponse(error, endTime - startTime));
                });

        } catch (Exception e) {
            long endTime = System.currentTimeMillis();
            return Mono.just(buildErrorResponse(e, endTime - startTime));
        }
    }

    /**
     * 构建成功响应
     */
    private Map<String, Object> buildSuccessResponse(String responseBody, long duration, int status, String statusText) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("status", status);
        result.put("statusText", statusText);
        result.put("duration", duration);
        result.put("body", responseBody);
        result.put("timestamp", System.currentTimeMillis());

        // 尝试解析响应大小
        result.put("size", responseBody.getBytes().length);

        return result;
    }

    /**
     * 构建错误响应
     */
    private Map<String, Object> buildErrorResponse(Throwable error, long duration) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("status", 0);
        result.put("statusText", "Request Failed");
        result.put("duration", duration);
        result.put("error", error.getMessage());
        result.put("timestamp", System.currentTimeMillis());

        return result;
    }

    /**
     * 测试请求模型
     */
    public static class TestRequest {
        private String method;
        private String url;
        private Map<String, String> headers;
        private String body;

        // Getters and Setters
        public String getMethod() { return method; }
        public void setMethod(String method) { this.method = method; }

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public Map<String, String> getHeaders() { return headers; }
        public void setHeaders(Map<String, String> headers) { this.headers = headers; }

        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
    }
}