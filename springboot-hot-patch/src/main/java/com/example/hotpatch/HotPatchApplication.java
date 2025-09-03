package com.example.hotpatch;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot 热补丁加载器主应用
 */
@SpringBootApplication
public class HotPatchApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(HotPatchApplication.class, args);
    }
}