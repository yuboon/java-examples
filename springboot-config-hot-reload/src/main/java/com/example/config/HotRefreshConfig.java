package com.example.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class HotRefreshConfig {
    @Bean
    public ConfigFileWatcher configFileWatcher(ConfigRefreshHandler refreshHandler) throws IOException {
        return new ConfigFileWatcher(refreshHandler);
    }
}