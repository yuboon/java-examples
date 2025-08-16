package com.example.logviewer.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

/**
 * 日志查看系统配置类
 * 
 * @author example
 * @version 1.0.0
 */
@Configuration
@ConfigurationProperties(prefix = "log.viewer")
@Data
public class LogConfig {
    
    /**
     * 日志文件根目录
     */
    private String logPath = "./logs";
    
    /**
     * 允许访问的日志文件扩展名
     */
    private List<String> allowedExtensions = Arrays.asList(".log", ".txt");
    
    /**
     * 单次查询最大行数
     */
    private int maxLines = 1000;
    
    /**
     * 文件最大大小（MB）
     */
    private long maxFileSize = 100;
    
    /**
     * 是否启用安全检查
     */
    private boolean enableSecurity = true;
}