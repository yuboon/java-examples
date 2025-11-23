package com.example.sign;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot API签名验证应用启动类
 *
 */
@Slf4j
@SpringBootApplication
public class ApiSignatureApplication {

    public static void main(String[] args) {
        SpringApplication.run(ApiSignatureApplication.class, args);
        log.info("========================================");
        log.info("Spring Boot API Signature Application Started Successfully!");
        log.info("API Base URL: http://localhost:8080/api");
        log.info("Health Check: http://localhost:8080/api/public/health");
        log.info("Public Info: http://localhost:8080/api/public/info");
        log.info("========================================");
    }
}