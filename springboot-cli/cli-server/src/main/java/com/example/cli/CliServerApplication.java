package com.example.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CLI服务端主程序
 */
@SpringBootApplication
public class CliServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(CliServerApplication.class, args);
    }
}