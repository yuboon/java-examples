package com.example.objectversion.config;

import org.javers.core.Javers;
import org.javers.core.JaversBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JaversConfig {

    @Bean
    public Javers javers() {
        return JaversBuilder.javers()
                .withPrettyPrint(true)
                .build();
    }
}
