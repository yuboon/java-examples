package com.example.dependencyscanner;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 依赖包扫描仪主启动类
 *
 */
@SpringBootApplication
public class DependencyScannerApplication {

    public static void main(String[] args) {
        SpringApplication.run(DependencyScannerApplication.class, args);
    }
}