package com.example.config;

import com.example.entity.ApplicationConfig;
import com.example.repository.ApplicationConfigRepository;
import com.example.service.ConfigEncryptionService;
import com.example.service.DynamicDataSourceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class DatabaseConfigurationLoader implements ApplicationListener<ApplicationReadyEvent> {

    @Autowired
    private ApplicationConfigRepository configRepository;
    
    @Autowired(required = false)
    private ConfigEncryptionService encryptionService;
    
    @Autowired
    private ConfigurableEnvironment environment;
    
    @Override
    public void onApplicationEvent(ApplicationReadyEvent event) {
        // 应用启动完成后，验证早期加载的配置
        verifyEarlyLoadedConfiguration();
    }
    
    /**
     * 验证早期加载的配置是否生效
     */
    private void verifyEarlyLoadedConfiguration() {
        try {
            String activeEnvironment = environment.getProperty("environment.active", "development");
            log.info("验证早期加载的数据库配置，当前环境: {}", activeEnvironment);
            
            // 检查关键配置是否已加载
            String serverPort = environment.getProperty("server.port");
            String datasourceUrl = environment.getProperty("spring.datasource.url");
            
            if (serverPort != null || datasourceUrl != null) {
                log.info("早期配置加载成功验证:");
                if (serverPort != null) {
                    log.info("  - server.port: {}", serverPort);
                }
                if (datasourceUrl != null) {
                    log.info("  - spring.datasource.url: {}", datasourceUrl.replaceAll("password=[^&]*", "password=******"));
                }
            } else {
                log.warn("早期配置加载可能失败，未找到预期的配置项");
            }
            
            // 获取所有配置用于验证
            List<ApplicationConfig> configs = configRepository.findAllActiveConfigsByEnvironment(activeEnvironment);
            
            // 记录需要重启的配置变更
            logRestartRequiredConfigs(configs);
            
        } catch (Exception e) {
            log.error("验证早期配置时发生错误", e);
        }
    }
    
    /**
     * 手动重新加载配置（用于配置更新后的热加载）
     * 注意：这个方法主要用于运行时配置刷新，无法影响server.port等需要在启动时生效的配置
     */
    public void reloadConfiguration() {
        try {
            String activeEnvironment = environment.getProperty("environment.active", "development");
            log.info("手动重新加载数据库配置，当前环境: {}", activeEnvironment);
            
            List<ApplicationConfig> configs = configRepository.findAllActiveConfigsByEnvironment(activeEnvironment);
            
            if (configs.isEmpty()) {
                log.warn("数据库中没有找到环境 {} 的配置数据", activeEnvironment);
                return;
            }
            
            Map<String, Object> databaseProperties = new HashMap<>();
            
            for (ApplicationConfig config : configs) {
                String configValue = config.getConfigValue();
                
                // 解密配置值
                if (Boolean.TRUE.equals(config.getEncrypted()) && encryptionService != null) {
                    try {
                        configValue = encryptionService.decrypt(configValue);
                        log.debug("解密配置: {}", config.getConfigKey());
                    } catch (Exception e) {
                        log.error("解密配置失败: {}, 使用原始值", config.getConfigKey(), e);
                    }
                }
                
                // 根据配置类型处理不同的配置
                String propertyKey = buildPropertyKey(config);
                databaseProperties.put(propertyKey, configValue);
                
                log.debug("重新加载配置: {} = {} (类型: {})", propertyKey, 
                    Boolean.TRUE.equals(config.getEncrypted()) ? "***encrypted***" : configValue, 
                    config.getConfigType());
            }
            
            // 移除旧的配置属性源
            environment.getPropertySources().remove("databaseConfiguration");
            
            // 将数据库配置添加到Spring环境中
            MapPropertySource databasePropertySource = new MapPropertySource("databaseConfiguration", databaseProperties);
            environment.getPropertySources().addFirst(databasePropertySource);
            
            log.info("成功重新加载 {} 个配置项", configs.size());
            
            // 记录需要重启的配置变更
            logRestartRequiredConfigs(configs);
            
        } catch (Exception e) {
            log.error("重新加载配置时发生错误", e);
            throw new RuntimeException("配置重新加载失败", e);
        }
    }
    
    private String buildPropertyKey(ApplicationConfig config) {
        String configKey = config.getConfigKey();
        String configType = config.getConfigType();
        
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
            case "kafka":
                if (!configKey.startsWith("spring.kafka")) {
                    return "spring.kafka." + configKey;
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
    
    private void logRestartRequiredConfigs(List<ApplicationConfig> configs) {
        List<ApplicationConfig> restartRequired = configs.stream()
            .filter(config -> Boolean.TRUE.equals(config.getRequiredRestart()))
            .toList();
            
        if (!restartRequired.isEmpty()) {
            log.warn("以下配置变更需要重启应用才能生效:");
            restartRequired.forEach(config -> 
                log.warn("  - {}: {} (类型: {})", 
                    config.getConfigKey(), 
                    config.getDescription(), 
                    config.getConfigType())
            );
        }
    }
    
}