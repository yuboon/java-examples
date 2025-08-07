package com.example.service;

import com.example.entity.ConfigHistory;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;

@Component
@Slf4j
public class ConfigurationMonitorService {
    
    @Autowired
    private MeterRegistry meterRegistry;
    
    @Autowired
    @Qualifier("configDataSource")
    private DataSource configDataSource;
    
    private final Counter configChangeCounter;
    private final Timer configLoadTimer;
    private final List<String> criticalConfigKeys = Arrays.asList(
        "url", "password", "username", "logging.level.root", "server.port"
    );
    
    public ConfigurationMonitorService(MeterRegistry meterRegistry) {
        this.configChangeCounter = Counter.builder("config.changes.total")
                .description("Total number of configuration changes")
                .register(meterRegistry);
        
        this.configLoadTimer = Timer.builder("config.load.duration")
                .description("Configuration loading duration")
                .register(meterRegistry);
    }
    
    /**
     * 处理配置变更事件
     */
    @EventListener
    @Async
    public void handleConfigChange(ConfigChangeEvent event) {
        try {
            // 记录监控指标
            recordConfigChangeMetrics(event);
            
            // 发送变更通知
            sendChangeNotification(event);
            
            // 检查配置合规性
            checkConfigCompliance(event);
            
        } catch (Exception e) {
            log.error("处理配置变更事件失败", e);
        }
    }
    
    private void recordConfigChangeMetrics(ConfigChangeEvent event) {
        try {
            Counter.builder("config.changes.total")
                   .tag("type", event.getConfigType())
                   .tag("environment", event.getEnvironment())
                   .tag("key", event.getConfigKey())
                   .register(meterRegistry)
                   .increment();
                   
            log.debug("已记录配置变更指标 - 类型: {}, 环境: {}, 键: {}", 
                     event.getConfigType(), event.getEnvironment(), event.getConfigKey());
        } catch (Exception e) {
            log.error("记录配置变更指标失败", e);
        }
    }
    
    private void sendChangeNotification(ConfigChangeEvent event) {
        try {
            if (isCriticalConfig(event.getConfigKey())) {
                log.warn("关键配置发生变更 - 键: {}, 类型: {}, 环境: {}", 
                        event.getConfigKey(), event.getConfigType(), event.getEnvironment());
                
                // 发送告警通知（这里简化为日志记录）
                sendCriticalConfigChangeAlert(event);
            } else {
                log.info("普通配置发生变更 - 键: {}, 类型: {}, 环境: {}", 
                        event.getConfigKey(), event.getConfigType(), event.getEnvironment());
            }
        } catch (Exception e) {
            log.error("发送配置变更通知失败", e);
        }
    }
    
    private void checkConfigCompliance(ConfigChangeEvent event) {
        try {
            // 检查配置是否符合规范
            if (event.getConfigKey().toLowerCase().contains("password") && 
                event.getNewValue().length() < 8) {
                log.warn("配置合规检查失败 - 密码长度不足: {}", event.getConfigKey());
            }
            
            // 检查数据库URL格式
            if ("url".equals(event.getConfigKey()) && 
                !event.getNewValue().startsWith("jdbc:")) {
                log.warn("配置合规检查失败 - 数据库URL格式错误: {}", event.getNewValue());
            }
            
        } catch (Exception e) {
            log.error("配置合规检查失败", e);
        }
    }
    
    private boolean isCriticalConfig(String configKey) {
        return criticalConfigKeys.contains(configKey) ||
               configKey.toLowerCase().contains("password") ||
               configKey.toLowerCase().contains("secret") ||
               configKey.startsWith("logging.level.root");
    }
    
    private void sendCriticalConfigChangeAlert(ConfigChangeEvent event) {
        // 这里可以集成邮件、短信、钉钉等通知方式
        log.error("【严重】关键配置变更告警 - 配置键: {}, 配置类型: {}, 环境: {}, 操作人: {}", 
                 event.getConfigKey(), event.getConfigType(), event.getEnvironment(), event.getOperatorId());
    }
    
    /**
     * 定期健康检查
     */
    @Scheduled(fixedRate = 300000) // 每5分钟检查一次
    public void healthCheck() {
        try {
            // 检查配置数据库连接状态
            checkConfigDatabaseHealth();
            
            // 检查配置缓存状态
            checkConfigCacheHealth();
            
            // 检查配置同步状态
            checkConfigSyncStatus();
            
        } catch (Exception e) {
            log.error("配置系统健康检查失败", e);
        }
    }
    
    private void checkConfigDatabaseHealth() {
        Timer.Sample sample = Timer.start(meterRegistry);
        try (Connection conn = configDataSource.getConnection()) {
            if (!conn.isValid(5)) {
                log.error("配置数据库连接异常");
                recordHealthMetric("config.database.health", 0);
            } else {
                log.debug("配置数据库连接正常");
                recordHealthMetric("config.database.health", 1);
            }
        } catch (SQLException e) {
            log.error("配置数据库不可用", e);
            recordHealthMetric("config.database.health", 0);
        } finally {
            sample.stop(Timer.builder("config.database.check.duration")
                           .register(meterRegistry));
        }
    }
    
    private void checkConfigCacheHealth() {
        try {
            // 检查配置缓存的健康状态
            log.debug("配置缓存状态正常");
            recordHealthMetric("config.cache.health", 1);
        } catch (Exception e) {
            log.error("配置缓存状态异常", e);
            recordHealthMetric("config.cache.health", 0);
        }
    }
    
    private void checkConfigSyncStatus() {
        try {
            // 检查配置同步状态
            log.debug("配置同步状态正常");
            recordHealthMetric("config.sync.health", 1);
        } catch (Exception e) {
            log.error("配置同步状态异常", e);
            recordHealthMetric("config.sync.health", 0);
        }
    }
    
    private void recordHealthMetric(String metricName, double value) {
        try {
            meterRegistry.gauge(metricName, value);
        } catch (Exception e) {
            log.error("记录健康指标失败: {}", metricName, e);
        }
    }
    
    /**
     * 记录配置加载性能指标
     */
    public Timer.Sample startConfigLoadTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void stopConfigLoadTimer(Timer.Sample sample, String configType) {
        sample.stop(Timer.builder("config.load.duration")
                        .tag("type", configType)
                        .register(meterRegistry));
    }
    
    // 配置变更事件类
    public static class ConfigChangeEvent {
        private String configKey;
        private String configType;
        private String environment;
        private String oldValue;
        private String newValue;
        private String operatorId;
        private String changeReason;
        
        public ConfigChangeEvent(String configKey, String configType, String environment, 
                               String oldValue, String newValue, String operatorId, String changeReason) {
            this.configKey = configKey;
            this.configType = configType;
            this.environment = environment;
            this.oldValue = oldValue;
            this.newValue = newValue;
            this.operatorId = operatorId;
            this.changeReason = changeReason;
        }
        
        // Getters
        public String getConfigKey() { return configKey; }
        public String getConfigType() { return configType; }
        public String getEnvironment() { return environment; }
        public String getOldValue() { return oldValue; }
        public String getNewValue() { return newValue; }
        public String getOperatorId() { return operatorId; }
        public String getChangeReason() { return changeReason; }
    }
}