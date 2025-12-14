package com.example.cli;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * CLI客户端主程序
 */
@SpringBootApplication
public class CliClientApplication {
    public static void main(String[] args) {
        SpringApplication.run(CliClientApplication.class, args);
    }
}