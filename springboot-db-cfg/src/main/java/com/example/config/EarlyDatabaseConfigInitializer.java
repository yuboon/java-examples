package com.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.MutablePropertySources;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

/**
 * 早期数据库配置初始化器
 * 使用ApplicationContextInitializer在Spring容器初始化的早期阶段加载数据库配置
 */
@Slf4j
public class EarlyDatabaseConfigInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        ConfigurableEnvironment environment = applicationContext.getEnvironment();
        
        try {
            log.info("开始早期数据库配置加载...");
            
            // 从数据库加载配置
            Map<String, Object> dynamicProperties = loadDynamicProperties(environment);
            
            if (!dynamicProperties.isEmpty()) {
                // 创建一个高优先级的属性源
                MapPropertySource dynamicPropertySource = new MapPropertySource("earlyDatabaseConfiguration", dynamicProperties);
                
                // 添加到环境中，确保它具有最高优先级
                MutablePropertySources propertySources = environment.getPropertySources();
                propertySources.addFirst(dynamicPropertySource);

                /*// 遍历所有 logging.level.* 并实时生效
                LoggingSystem loggingSystem = LoggingSystem.get(LoggingSystem.class.getClassLoader());
                dynamicProperties.entrySet().stream()
                        .filter(e -> e.getKey().startsWith("logging.level."))
                        .forEach(e -> {
                            String loggerName = e.getKey().substring("logging.level.".length());
                            LogLevel level = LogLevel.valueOf(e.getValue().toString().toUpperCase());
                            loggingSystem.setLogLevel(loggerName, level);   // 毫秒级
                        });*/
                
                log.info("成功从数据库加载 {} 个早期配置项", dynamicProperties.size());
                
                // 记录重要的配置
                dynamicProperties.forEach((key, value) -> {
                    if (key.contains("port") || key.contains("server") || key.contains("datasource")) {
                        log.info("早期加载配置: {} = {}", key, isPasswordField(key) ? "******" : value);
                    }
                });
            } else {
                log.warn("数据库中没有找到早期配置数据");
            }
            
        } catch (Exception e) {
            log.error("早期数据库配置加载失败，将使用默认配置", e);
            // 不抛异常，允许应用使用默认配置启动
        }
    }
    
    private Map<String, Object> loadDynamicProperties(ConfigurableEnvironment environment) {
        Map<String, Object> properties = new HashMap<>();
        Map<String, String> loggingConfigs = new HashMap<>();
        
        try {
            // 从环境变量或默认值获取配置
            String username = environment.getProperty("spring.config-datasource.username", "root");
            String password = environment.getProperty("spring.config-datasource.password", "root");
            // 构建JDBC URL
            String jdbcUrl = environment.getProperty("spring.config-datasource.url");
            // 获取当前环境
            String activeEnvironment = System.getProperty("spring.profiles.active", 
                                      System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "development"));
            
            // 连接数据库查询配置
            try (Connection conn = DriverManager.getConnection(jdbcUrl, username, password)) {
                log.debug("成功连接到配置数据库: {}", jdbcUrl);
                
                String sql = "SELECT config_key, config_value, config_type FROM application_config WHERE environment = ? AND active = true ORDER BY config_type, config_key";
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setString(1, activeEnvironment);
                    
                    try (ResultSet rs = stmt.executeQuery()) {
                        while (rs.next()) {
                            String configKey = rs.getString("config_key");
                            String configValue = rs.getString("config_value");
                            String configType = rs.getString("config_type");
                            
                            // 构建完整的属性键
                            String propertyKey = buildPropertyKey(configKey, configType);
                            properties.put(propertyKey, configValue);
                            
                            // 收集日志配置以便后续应用
                            if (propertyKey.contains("logging.level")) {
                                // 提取日志器名称和级别
                                String loggerName = extractLoggerName(configKey);
                                if (loggerName != null && !loggerName.isEmpty()) {
                                    loggingConfigs.put(loggerName, configValue);
                                }
                            }
                            
                            log.debug("早期加载配置: {} = {} (类型: {})", propertyKey, 
                                isPasswordField(configKey) ? "******" : configValue, configType);
                        }
                    }
                }
            }
            
            // 应用日志级别配置
            if (!loggingConfigs.isEmpty()) {
                applyLoggingConfigurations(loggingConfigs);
                log.info("早期应用了 {} 个日志级别配置", loggingConfigs.size());
            }
            
        } catch (Exception e) {
            log.error("查询数据库配置时发生错误", e);
            throw new RuntimeException("数据库配置查询失败", e);
        }
        
        return properties;
    }
    
    /**
     * 提取日志器名称
     */
    private String extractLoggerName(String configKey) {
        // 处理类似 "level.com.example" 或 "level.ROOT" 的配置键
        if (configKey.startsWith("logging.level.")) {
            return configKey.substring("logging.level.".length()); // 移除 "level." 前缀
        }
        return null;
    }
    
    /**
     * 早期应用日志级别配置
     */
    private void applyLoggingConfigurations(Map<String, String> loggingConfigs) {
        try {
            // 在早期阶段直接使用LogBack API设置日志级别
            ch.qos.logback.classic.LoggerContext loggerContext = 
                (ch.qos.logback.classic.LoggerContext) org.slf4j.LoggerFactory.getILoggerFactory();
            
            for (Map.Entry<String, String> entry : loggingConfigs.entrySet()) {
                String loggerName = entry.getKey();
                String levelStr = entry.getValue();
                
                try {
                    // 获取或创建Logger
                    ch.qos.logback.classic.Logger logger = "ROOT".equals(loggerName) 
                        ? loggerContext.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME)
                        : loggerContext.getLogger(loggerName);
                    
                    // 解析和设置日志级别
                    ch.qos.logback.classic.Level level = ch.qos.logback.classic.Level.valueOf(levelStr.toUpperCase());
                    logger.setLevel(level);
                    
                    log.info("早期设置日志级别: {} = {}", loggerName, levelStr);
                    
                } catch (Exception e) {
                    log.warn("设置日志级别失败: {} = {}, 错误: {}", loggerName, levelStr, e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("早期应用日志级别配置失败", e);
        }
    }
    
    private String buildPropertyKey(String configKey, String configType) {
        if (configType == null) {
            return configKey;
        }
        
        // 根据配置类型构建完整的属性键
        switch (configType.toLowerCase()) {
            case "datasource":
                if (!configKey.startsWith("spring.datasource")) {
                    return "spring.datasource." + configKey;
                }
                break;
            case "redis":
                if (!configKey.startsWith("spring.redis")) {
                    return "spring.redis." + configKey;
                }
                break;
            case "server":
                if (!configKey.startsWith("server")) {
                    return "server." + configKey;
                }
                break;
            case "logging":
                if (!configKey.startsWith("logging")) {
                    return "logging." + configKey;
                }
                break;
            case "management":
                if (!configKey.startsWith("management")) {
                    return "management." + configKey;
                }
                break;
            case "framework":
            case "business":
            default:
                // 对于框架级和业务配置，直接使用原始键名
                break;
        }
        
        return configKey;
    }
    
    private boolean isPasswordField(String key) {
        String lowerKey = key.toLowerCase();
        return lowerKey.contains("password") || lowerKey.contains("passwd") || 
               lowerKey.contains("secret") || lowerKey.contains("key") ||
               lowerKey.contains("token");
    }
}