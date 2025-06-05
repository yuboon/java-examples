package com.example.livestream;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.livestream.mapper")
public class LiveApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(LiveApplication.class, args);
    }
}