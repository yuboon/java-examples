package com.example.encryption;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ColumnEncryptionApplication {

    public static void main(String[] args) {
        SpringApplication.run(ColumnEncryptionApplication.class, args);
        System.out.println("🚀 Spring Boot 字段级加密演示项目启动成功！");
        System.out.println("📱 前端访问地址: http://localhost:8080");
        System.out.println("🔧 API文档地址: http://localhost:8080/api/users");
    }
}