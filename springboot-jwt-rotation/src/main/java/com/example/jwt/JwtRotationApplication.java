package com.example.jwt;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class JwtRotationApplication {
    public static void main(String[] args) {
        SpringApplication.run(JwtRotationApplication.class, args);
    }
}