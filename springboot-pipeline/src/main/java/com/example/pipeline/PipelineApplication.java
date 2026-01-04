package com.example.pipeline;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 执行管道模式示例应用
 */
@SpringBootApplication
public class PipelineApplication {

    public static void main(String[] args) {
        SpringApplication.run(PipelineApplication.class, args);
        System.out.println("""
                ========================================
                Spring Boot Pipeline 应用已启动!

                访问示例：
                POST http://localhost:8080/api/orders

                请求体示例：
                {
                  "user_id": 1,
                  "product_id": 100,
                  "product_name": "iPhone 15 Pro",
                  "quantity": 1,
                  "unit_price": 7999.00,
                  "address": "北京市朝阳区",
                  "remark": "尽快发货",
                  "source": "WEB"
                }
                ========================================
                """);
    }
}
