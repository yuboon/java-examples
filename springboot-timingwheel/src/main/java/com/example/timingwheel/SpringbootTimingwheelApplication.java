package com.example.timingwheel;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot应用程序主类
 */
@SpringBootApplication
public class SpringbootTimingwheelApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringbootTimingwheelApplication.class, args);
        System.out.println("=========================================");
        System.out.println("Timing Wheel Application Started!");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("API文档: http://localhost:8080/api/timingwheel/stats");
        System.out.println("=========================================");
    }
}