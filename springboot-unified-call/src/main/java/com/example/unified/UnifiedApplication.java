package com.example.unified;

import org.apache.dubbo.config.spring.context.annotation.EnableDubbo;
import org.apache.dubbo.config.utils.SimpleReferenceCache;
import org.apache.dubbo.rpc.Filter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
@EnableDubbo
public class UnifiedApplication {

    public static void main(String[] args) {
        SpringApplication.run(UnifiedApplication.class, args);
    }


    @Bean
    public SimpleReferenceCache simpleReferenceCache() {
        return SimpleReferenceCache.getCache();
    }

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}