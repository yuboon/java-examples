package com.example.service;

import com.example.entity.ApplicationConfig;
import com.example.entity.ConfigHistory;
import com.example.repository.ApplicationConfigRepository;
import com.example.repository.ConfigHistoryRepository;
import com.example.utils.EnvironmentUtil;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DynamicConfigurationService {
    
    @Autowired
    private ApplicationConfigRepository configRepository;
    
    @Autowired
    private ConfigHistoryRepository historyRepository;
    
    @Autowired
    private ConfigurableApplicationContext applicationContext;
    
    @Autowired
    private ConfigEncryptionService encryptionService;
    
    @Autowired
    private EnvironmentUtil environmentUtil;
    
    private static final String DYNAMIC_PROPERTY_SOURCE_NAME = "dynamicConfigPropertySource";
    private static final String FRAMEWORK_PROPERTY_SOURCE_NAME = "frameworkConfigPropertySource";
    
    /**
     * 动态更新数据源配置
     */
    public void updateDataSourceConfig(String environment, Map<String, String> newConfig) {
        try {
            log.info("开始更新数据源配置 - 环境: {}", environment);
            
            // 1. 验证配置有效性
            validateDataSourceConfig(newConfig);
            
            // 2. 保存到数据库
            saveConfigToDatabase("datasource", environment, newConfig);
            
            // 3. 创建新的数据源
            DataSource newDataSource = createDataSource(newConfig);
            
            // 4. 优雅替换现有数据源
            replaceDataSource(newDataSource);
            
            // 5. 发布配置变更事件
            publishConfigChangeEvent("datasource", newConfig);
            
            log.info("数据源配置更新成功");
        } catch (Exception e) {
            log.error("更新数据源配置失败", e);
            throw new RuntimeException("Failed to update datasource configuration", e);
        }
    }
    
    /**
     * 动态更新Redis配置
     */
    public void updateRedisConfig(String environment, Map<String, String> newConfig) {
        try {
            log.info("开始更新Redis配置 - 环境: {}", environment);
            
            saveConfigToDatabase("redis", environment, newConfig);
            
            // 重新创建Redis连接工厂
            LettuceConnectionFactory newFactory = createRedisConnectionFactory(newConfig);
            replaceRedisConnectionFactory(newFactory);
            
            publishConfigChangeEvent("redis", newConfig);
            
            log.info("Redis配置更新成功");
        } catch (Exception e) {
            log.error("更新Redis配置失败", e);
            throw new RuntimeException("Failed to update redis configuration", e);
        }
    }
    
    /**
     * 动态更新业务配置
     */
    public void updateBusinessConfig(String configKey, String configValue) {
        String environment = environmentUtil.getCurrentEnvironment();
        try {
            log.info("开始更新业务配置 - Key: {}, 环境: {}", configKey, environment);
            
            Optional<ApplicationConfig> existingConfig = configRepository
                .findByConfigKeyAndEnvironmentAndConfigTypeAndActiveTrue(configKey, environment, "business");
            
            ApplicationConfig config = existingConfig.orElse(new ApplicationConfig());
            String oldValue = config.getConfigValue();
            
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigType("business");
            config.setEnvironment(environment);
            config.setActive(true);
            
            // 如果是敏感配置，则加密存储
            if (encryptionService.isSensitiveConfig(configKey)) {
                config.setConfigValue(encryptionService.encryptSensitiveConfig(configValue));
                config.setEncrypted(true);
            }
            
            configRepository.save(config);
            
            // 记录变更历史
            recordConfigChange(configKey, "business", oldValue, configValue, environment, "系统自动更新");
            
            // 更新Environment中的属性
            updateEnvironmentProperty(configKey, configValue);
            
            publishConfigChangeEvent("business", Map.of(configKey, configValue));
            
            log.info("业务配置更新成功");
        } catch (Exception e) {
            log.error("更新业务配置失败", e);
            throw new RuntimeException("Failed to update business configuration", e);
        }
    }
    
    /**
     * 动态更新框架配置（如日志级别等）
     */
    public void updateFrameworkConfig(String configKey, String configValue) {
        String environment = environmentUtil.getCurrentEnvironment();
        try {
            log.info("开始更新框架配置 - Key: {}, Value: {}, 环境: {}", configKey, configValue, environment);
            
            Optional<ApplicationConfig> existingConfig = configRepository
                .findByConfigKeyAndEnvironmentAndConfigTypeAndActiveTrue(configKey, environment, "framework");
            
            ApplicationConfig config = existingConfig.orElse(new ApplicationConfig());
            String oldValue = config.getConfigValue();
            
            config.setConfigKey(configKey);
            config.setConfigValue(configValue);
            config.setConfigType("framework");
            config.setEnvironment(environment);
            config.setActive(true);
            
            configRepository.save(config);
            
            // 记录变更历史
            recordConfigChange(configKey, "framework", oldValue, configValue, environment, "框架配置更新");
            
            // 更新Environment中的框架属性
            updateFrameworkEnvironmentProperty(configKey, configValue);
            
            // 如果是日志配置，特殊处理
            if (configKey.startsWith("logging.level.")) {
                updateLoggingLevel(configKey, configValue);
            }
            
            publishConfigChangeEvent("framework", Map.of(configKey, configValue));
            
            log.info("框架配置更新成功");
        } catch (Exception e) {
            log.error("更新框架配置失败", e);
            throw new RuntimeException("Failed to update framework configuration", e);
        }
    }
    
    /**
     * 更新Environment中的普通属性
     */
    private void updateEnvironmentProperty(String key, String value) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        
        // 获取或创建动态属性源
        MapPropertySource dynamicPropertySource = (MapPropertySource) propertySources.get(DYNAMIC_PROPERTY_SOURCE_NAME);
        if (dynamicPropertySource == null) {
            Map<String, Object> dynamicProperties = new HashMap<>();
            dynamicPropertySource = new MapPropertySource(DYNAMIC_PROPERTY_SOURCE_NAME, dynamicProperties);
            propertySources.addFirst(dynamicPropertySource);
        }
        
        // 更新属性值
        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) dynamicPropertySource.getSource();
        source.put(key, value);
        
        log.info("已更新Environment属性: {} = {}", key, value);
    }
    
    /**
     * 更新Environment中的框架属性
     */
    private void updateFrameworkEnvironmentProperty(String key, String value) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        MutablePropertySources propertySources = environment.getPropertySources();
        
        // 获取或创建框架属性源，优先级更高
        MapPropertySource frameworkPropertySource = (MapPropertySource) propertySources.get(FRAMEWORK_PROPERTY_SOURCE_NAME);
        if (frameworkPropertySource == null) {
            Map<String, Object> frameworkProperties = new HashMap<>();
            frameworkPropertySource = new MapPropertySource(FRAMEWORK_PROPERTY_SOURCE_NAME, frameworkProperties);
            propertySources.addFirst(frameworkPropertySource);
        }
        
        // 更新框架属性值
        @SuppressWarnings("unchecked")
        Map<String, Object> source = (Map<String, Object>) frameworkPropertySource.getSource();
        source.put(key, value);
        
        log.info("已更新Framework Environment属性: {} = {}", key, value);
    }
    
    /**
     * 动态更新日志级别
     */
    private void updateLoggingLevel(String configKey, String configValue) {
        try {
            // 提取logger名称，例如 logging.level.com.example -> com.example
            String loggerName = configKey.substring("logging.level.".length());
            
            // 获取日志系统并更新级别
            ch.qos.logback.classic.Logger logger = (ch.qos.logback.classic.Logger) 
                org.slf4j.LoggerFactory.getLogger(loggerName);
            
            ch.qos.logback.classic.Level level = ch.qos.logback.classic.Level.valueOf(configValue.toUpperCase());
            logger.setLevel(level);
            
            log.info("已更新日志级别: {} -> {}", loggerName, configValue);
        } catch (Exception e) {
            log.error("更新日志级别失败: {} = {}", configKey, configValue, e);
        }
    }
    
    /**
     * 批量更新框架配置
     */
    public void updateFrameworkConfigs(String environment, Map<String, String> configMap) {
        try {
            log.info("开始批量更新框架配置 - 环境: {}, 配置数量: {}", environment, configMap.size());
            
            for (Map.Entry<String, String> entry : configMap.entrySet()) {
                updateFrameworkConfig(entry.getKey(), entry.getValue());
            }
            
            log.info("批量更新框架配置成功");
        } catch (Exception e) {
            log.error("批量更新框架配置失败", e);
            throw new RuntimeException("Failed to batch update framework configurations", e);
        }
    }
    
    private void validateDataSourceConfig(Map<String, String> config) {
        if (!config.containsKey("url")) {
            throw new IllegalArgumentException("数据源URL不能为空");
        }
        if (!config.containsKey("username")) {
            throw new IllegalArgumentException("数据源用户名不能为空");
        }
        if (!config.containsKey("password")) {
            throw new IllegalArgumentException("数据源密码不能为空");
        }
    }
    
    private void saveConfigToDatabase(String configType, String environment, Map<String, String> configMap) {
        for (Map.Entry<String, String> entry : configMap.entrySet()) {
            String configKey = entry.getKey();
            String configValue = entry.getValue();
            
            Optional<ApplicationConfig> existingConfig = configRepository
                .findByConfigKeyAndEnvironmentAndConfigTypeAndActiveTrue(configKey, environment, configType);
            
            ApplicationConfig config = existingConfig.orElse(new ApplicationConfig());
            String oldValue = config.getConfigValue();
            
            config.setConfigKey(configKey);
            config.setConfigType(configType);
            config.setEnvironment(environment);
            config.setActive(true);
            
            // 处理加密
            if (encryptionService.isSensitiveConfig(configKey)) {
                config.setConfigValue(encryptionService.encryptSensitiveConfig(configValue));
                config.setEncrypted(true);
            } else {
                config.setConfigValue(configValue);
                config.setEncrypted(false);
            }
            
            configRepository.save(config);
            
            // 记录变更历史
            recordConfigChange(configKey, configType, oldValue, configValue, environment, "配置更新");
        }
    }
    
    private DataSource createDataSource(Map<String, String> config) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setJdbcUrl(config.get("url"));
        hikariConfig.setUsername(config.get("username"));
        hikariConfig.setPassword(config.get("password"));
        hikariConfig.setDriverClassName(config.getOrDefault("driver-class-name", "com.mysql.cj.jdbc.Driver"));
        hikariConfig.setMaximumPoolSize(Integer.parseInt(config.getOrDefault("maximum-pool-size", "20")));
        hikariConfig.setMinimumIdle(Integer.parseInt(config.getOrDefault("minimum-idle", "5")));
        hikariConfig.setConnectionTimeout(Long.parseLong(config.getOrDefault("connection-timeout", "30000")));
        hikariConfig.setIdleTimeout(Long.parseLong(config.getOrDefault("idle-timeout", "600000")));
        hikariConfig.setMaxLifetime(Long.parseLong(config.getOrDefault("max-lifetime", "1800000")));
        
        return new HikariDataSource(hikariConfig);
    }
    
    private void replaceDataSource(@Qualifier("businessDataSource") DataSource newDataSource) {
        DefaultListableBeanFactory beanFactory = 
            (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        
        try {
            // 优雅关闭旧数据源
            if (beanFactory.containsBean("businessDataSource")) {
                DataSource oldDataSource = beanFactory.getBean("businessDataSource", DataSource.class);
                if (oldDataSource instanceof HikariDataSource) {
                    ((HikariDataSource) oldDataSource).close();
                }
            }
            
            // 注册新数据源
            beanFactory.destroySingleton("businessDataSource");
            beanFactory.registerSingleton("businessDataSource", newDataSource);
            
            log.info("数据源替换成功");
        } catch (Exception e) {
            log.error("替换数据源失败", e);
            throw new RuntimeException("Failed to replace datasource", e);
        }
    }
    
    private LettuceConnectionFactory createRedisConnectionFactory(Map<String, String> config) {
        // 创建Redis连接工厂的实现
        log.info("创建Redis连接工厂: {}", config);
        return null; // 简化实现
    }
    
    private void replaceRedisConnectionFactory(LettuceConnectionFactory newFactory) {
        // Redis连接工厂替换的实现
        log.info("替换Redis连接工厂");
    }
    
    private void recordConfigChange(String configKey, String configType, String oldValue, String newValue, String environment, String reason) {
        try {
            ConfigHistory history = new ConfigHistory();
            history.setConfigKey(configKey);
            history.setConfigType(configType);
            history.setOldValue(oldValue);
            history.setNewValue(newValue);
            history.setEnvironment(environment);
            history.setOperatorId("system"); // 可以从SecurityContext获取
            history.setChangeReason(reason);
            
            historyRepository.save(history);
        } catch (Exception e) {
            log.error("记录配置变更历史失败", e);
        }
    }
    
    private void publishConfigChangeEvent(String configType, Map<String, String> config) {
        // 发布配置变更事件
        log.info("发布配置变更事件 - 类型: {}, 配置: {}", configType, config.keySet());
    }
    
    /**
     * 获取配置变更历史
     */
    public List<ConfigHistory> getConfigHistory(String configKey, String environment) {
        return historyRepository.findByConfigKeyAndEnvironmentOrderByChangeTimeDesc(configKey, environment);
    }
    
    /**
     * 配置回滚功能
     */
    public void rollbackConfig(String configKey, String environment, Long historyId) {
        ConfigHistory history = historyRepository.findById(historyId)
            .orElseThrow(() -> new RuntimeException("历史记录不存在"));
        
        // 恢复到历史版本的值
        if ("business".equals(history.getConfigType())) {
            updateBusinessConfig(configKey, history.getOldValue());
        } else if ("framework".equals(history.getConfigType())) {
            updateFrameworkConfig(configKey, history.getOldValue());
        }
        
        log.info("配置已回滚 - Key: {}, 环境: {}, 回滚到版本: {}", 
                configKey, environment, historyId);
    }
}