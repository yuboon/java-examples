package com.example.multiport;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 多端口Spring Boot应用主类
 * 同时监听8082（用户端）和8083（管理端）端口
 */
@SpringBootApplication
public class MultiPortApplication {

    public static void main(String[] args) {
        SpringApplication.run(MultiPortApplication.class, args);
    }
}