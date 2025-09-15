package com.example.jarconflict;

import com.example.jarconflict.advisor.ConflictAdvisor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ConflictAdvisor.class)
public class JarConflictDetectorApplication {

    public static void main(String[] args) {
        SpringApplication.run(JarConflictDetectorApplication.class, args);
    }
}