package com.example.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

@RestController
public class TestController {

    @Autowired
    private RestTemplate customRestTemplate;  // 注入自定义的RestTemplate

    @GetMapping("/call-api")
    public String callThirdApi() {
        // 调用第三方API，拦截器自动处理认证、日志、重试
        return customRestTemplate.getForObject("http://localhost:8080/mock-third-api", String.class);
    }

    // 模拟一个三方接口
    @GetMapping("/mock-third-api")
    public String mockThirdApi() {
        return "hello";
    }
}