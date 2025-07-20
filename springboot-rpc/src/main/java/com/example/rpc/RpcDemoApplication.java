package com.example.rpc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableAutoConfiguration
public class RpcDemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(RpcDemoApplication.class, args);
    }
}