package com.apidoc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * API文档工具主启动类
 *
 */
@SpringBootApplication
public class ApiDocApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiDocApplication.class, args);
        System.out.println();
        System.out.println("🚀 API文档工具启动成功!");
        System.out.println("📖 访问地址: http://localhost:8080");
        System.out.println("🔧 轻量级 • 零配置 • 开箱即用");
        System.out.println();
    }
}