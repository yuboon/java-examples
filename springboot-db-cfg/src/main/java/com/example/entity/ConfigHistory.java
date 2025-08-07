package com.example.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigHistory {
    
    private Long id;
    private String configKey;
    private String configType;
    private String oldValue;
    private String newValue;
    private String environment;
    private String operatorId;
    private String changeReason;
    private LocalDateTime changeTime;
}