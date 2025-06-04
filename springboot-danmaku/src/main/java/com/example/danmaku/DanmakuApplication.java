package com.example.danmaku;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.danmaku.mapper")
public class DanmakuApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(DanmakuApplication.class, args);
    }
}