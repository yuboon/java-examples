package com.example.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationConfig {
    
    private Long id;
    private String configKey;
    private String configValue;
    private String configType; // datasource, redis, kafka, business, framework
    private String environment;
    private String description;
    private Boolean encrypted = false;
    private Boolean requiredRestart = false; // 是否需要重启应用
    private Boolean active = true;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}