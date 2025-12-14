package com.example.cli;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;

/**
 * CLI配置属性
 */
@Component
@ConfigurationProperties(prefix = "cli")
public class CliProperties {

    /**
     * 允许通过CLI访问的服务列表
     * 如果为空，则允许所有实现了CommandHandler的服务
     */
    private Set<String> allowedServices = new HashSet<>();

    /**
     * 是否启用命令执行日志
     */
    private boolean enableExecutionLog = true;

    /**
     * 命令执行超时时间（毫秒）
     */
    private long executionTimeout = 30000;

    public Set<String> getAllowedServices() {
        return allowedServices;
    }

    public void setAllowedServices(Set<String> allowedServices) {
        this.allowedServices = allowedServices;
    }

    public boolean isEnableExecutionLog() {
        return enableExecutionLog;
    }

    public void setEnableExecutionLog(boolean enableExecutionLog) {
        this.enableExecutionLog = enableExecutionLog;
    }

    public long getExecutionTimeout() {
        return executionTimeout;
    }

    public void setExecutionTimeout(long executionTimeout) {
        this.executionTimeout = executionTimeout;
    }
}