package com.example.controller;

import com.example.entity.ApplicationConfig;
import com.example.entity.ConfigHistory;
import com.example.repository.ApplicationConfigRepository;
import com.example.service.DynamicConfigurationService;
import com.example.service.DynamicDataSourceService;
import com.example.utils.EnvironmentUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.sql.DataSource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/config")
@Slf4j
public class ConfigController {
    
    @Autowired
    private DynamicConfigurationService configService;
    
    @Autowired
    private ApplicationConfigRepository configRepository;
    
    @Autowired
    private EnvironmentUtil environmentUtil;

    @Autowired(required = false)
    private DynamicDataSourceService dynamicDataSourceService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    /**
     * 安全地获取业务数据源（可能不存在）
     */
    private DataSource getBusinessDataSourceSafely() {
        try {
            return applicationContext.getBean("businessDataSource", DataSource.class);
        } catch (Exception e) {
            log.debug("业务数据源未找到: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * 获取指定类型的配置
     */
    @GetMapping("/{configType}/{environment}")
    public ResponseEntity<Map<String, String>> getConfig(
            @PathVariable String configType,
            @PathVariable String environment) {
        try {
            List<ApplicationConfig> configs = configRepository
                .findByConfigTypeAndEnvironmentAndActiveTrue(configType, environment);
                
            Map<String, String> configMap = configs.stream()
                .collect(Collectors.toMap(
                    ApplicationConfig::getConfigKey,
                    config -> Boolean.TRUE.equals(config.getEncrypted()) ? "******" : config.getConfigValue()
                ));
                
            return ResponseEntity.ok(configMap);
        } catch (Exception e) {
            log.error("获取配置失败", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 获取当前环境的所有配置
     */
    @GetMapping("/current")
    public ResponseEntity<Map<String, Map<String, String>>> getCurrentConfigs() {
        try {
            log.info("current...");
            String currentEnv = environmentUtil.getCurrentEnvironment();
            List<ApplicationConfig> allConfigs = configRepository
                .findAllActiveConfigsByEnvironment(currentEnv);
            
            Map<String, Map<String, String>> result = new HashMap<>();
            
            for (ApplicationConfig config : allConfigs) {
                result.computeIfAbsent(config.getConfigType(), k -> new HashMap<>())
                      .put(config.getConfigKey(), 
                           Boolean.TRUE.equals(config.getEncrypted()) ? "******" : config.getConfigValue());
            }
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("获取当前配置失败", e);
            return ResponseEntity.status(500).body(Map.of("error", Map.of("message", e.getMessage())));
        }
    }
    
    /**
     * 批量更新数据源配置
     */
    @PostMapping("/datasource/{environment}")
    public ResponseEntity<Map<String, String>> updateDataSourceConfig(
            @PathVariable String environment,
            @RequestBody Map<String, String> configMap) {
        try {
            configService.updateDataSourceConfig(environment, configMap);
            return ResponseEntity.ok(Map.of("message", "数据源配置更新成功"));
        } catch (Exception e) {
            log.error("更新数据源配置失败", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 批量更新Redis配置
     */
    @PostMapping("/redis/{environment}")
    public ResponseEntity<Map<String, String>> updateRedisConfig(
            @PathVariable String environment,
            @RequestBody Map<String, String> configMap) {
        try {
            configService.updateRedisConfig(environment, configMap);
            return ResponseEntity.ok(Map.of("message", "Redis配置更新成功"));
        } catch (Exception e) {
            log.error("更新Redis配置失败", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 更新单个业务配置
     */
    @PostMapping("/business")
    public ResponseEntity<Map<String, String>> updateBusinessConfig(
            @RequestBody ConfigUpdateRequest request) {
        try {
            configService.updateBusinessConfig(request.getConfigKey(), request.getConfigValue());
            return ResponseEntity.ok(Map.of("message", "业务配置更新成功"));
        } catch (Exception e) {
            log.error("更新业务配置失败", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 更新单个框架配置（如日志级别等）
     */
    @PostMapping("/framework")
    public ResponseEntity<Map<String, String>> updateFrameworkConfig(
            @RequestBody ConfigUpdateRequest request) {
        try {
            configService.updateFrameworkConfig(request.getConfigKey(), request.getConfigValue());
            return ResponseEntity.ok(Map.of("message", "框架配置更新成功"));
        } catch (Exception e) {
            log.error("更新框架配置失败", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 批量更新框架配置
     */
    @PostMapping("/framework/{environment}")
    public ResponseEntity<Map<String, String>> updateFrameworkConfigs(
            @PathVariable String environment,
            @RequestBody Map<String, String> configMap) {
        try {
            configService.updateFrameworkConfigs(environment, configMap);
            return ResponseEntity.ok(Map.of("message", "框架配置批量更新成功"));
        } catch (Exception e) {
            log.error("批量更新框架配置失败", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 获取配置变更历史
     */
    @GetMapping("/history/{configKey}")
    public ResponseEntity<List<ConfigHistory>> getConfigHistory(
            @PathVariable String configKey,
            @RequestParam(defaultValue = "") String environment) {
        try {
            String env = environment.isEmpty() ? environmentUtil.getCurrentEnvironment() : environment;
            List<ConfigHistory> history = configService.getConfigHistory(configKey, env);
            return ResponseEntity.ok(history);
        } catch (Exception e) {
            log.error("获取配置历史失败", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 配置回滚
     */
    @PostMapping("/rollback")
    public ResponseEntity<Map<String, String>> rollbackConfig(@RequestBody RollbackRequest request) {
        try {
            configService.rollbackConfig(request.getConfigKey(), request.getEnvironment(), request.getHistoryId());
            return ResponseEntity.ok(Map.of("message", "配置回滚成功"));
        } catch (Exception e) {
            log.error("配置回滚失败", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 获取所有配置类型
     */
    @GetMapping("/types")
    public ResponseEntity<List<String>> getConfigTypes() {
        try {
            List<String> types = configRepository.findDistinctConfigTypes();
            return ResponseEntity.ok(types);
        } catch (Exception e) {
            log.error("获取配置类型失败", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 获取所有环境
     */
    @GetMapping("/environments")
    public ResponseEntity<List<String>> getEnvironments() {
        try {
            List<String> environments = configRepository.findDistinctEnvironments();
            return ResponseEntity.ok(environments);
        } catch (Exception e) {
            log.error("获取环境列表失败", e);
            return ResponseEntity.status(500).build();
        }
    }
    
    /**
     * 健康检查
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        try {
            Map<String, Object> health = new HashMap<>();
            health.put("status", "UP");
            health.put("currentEnvironment", environmentUtil.getCurrentEnvironment());
            health.put("timestamp", System.currentTimeMillis());
            health.put("businessDataSourceAvailable", dynamicDataSourceService != null ? dynamicDataSourceService.isBusinessDataSourceAvailable() : false);
            
            return ResponseEntity.ok(health);
        } catch (Exception e) {
            log.error("健康检查失败", e);
            Map<String, Object> health = new HashMap<>();
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            return ResponseEntity.status(500).body(health);
        }
    }
    
    /**
     * 手动刷新动态数据源
     */
    @PostMapping("/datasource/refresh")
    public ResponseEntity<Map<String, String>> refreshDataSource() {
        try {
            if (dynamicDataSourceService != null) {
                dynamicDataSourceService.updateBusinessDataSource();
                return ResponseEntity.ok(Map.of("message", "动态数据源刷新成功"));
            } else {
                return ResponseEntity.status(500).body(Map.of("error", "动态数据源服务不可用"));
            }
        } catch (Exception e) {
            log.error("刷新动态数据源失败", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    /**
     * 测试业务数据源连接状态
     */
    @GetMapping("/datasource/business/test")
    public ResponseEntity<Map<String, Object>> testBusinessDataSourceConnection() {
        try {
            DataSource businessDataSource = getBusinessDataSourceSafely();
            Map<String, Object> result = new HashMap<>();
            
            if (businessDataSource == null) {
                result.put("available", false);
                result.put("message", "业务数据源未创建");
                result.put("timestamp", System.currentTimeMillis());
                return ResponseEntity.ok(result);
            }
            
            // 测试连接
            try (var connection = businessDataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                result.put("available", isValid);
                result.put("message", isValid ? "业务数据源连接正常" : "业务数据源连接失败");
                result.put("timestamp", System.currentTimeMillis());
                result.put("dataSourceClass", businessDataSource.getClass().getSimpleName());
                return ResponseEntity.ok(result);
            }
            
        } catch (Exception e) {
            log.error("测试业务数据源连接失败", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    
    // 内部类定义请求对象
    public static class ConfigUpdateRequest {
        private String configKey;
        private String configValue;
        private String description;
        
        // Getters and Setters
        public String getConfigKey() { return configKey; }
        public void setConfigKey(String configKey) { this.configKey = configKey; }
        public String getConfigValue() { return configValue; }
        public void setConfigValue(String configValue) { this.configValue = configValue; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
    }
    
    public static class RollbackRequest {
        private String configKey;
        private String environment;
        private Long historyId;
        
        // Getters and Setters
        public String getConfigKey() { return configKey; }
        public void setConfigKey(String configKey) { this.configKey = configKey; }
        public String getEnvironment() { return environment; }
        public void setEnvironment(String environment) { this.environment = environment; }
        public Long getHistoryId() { return historyId; }
        public void setHistoryId(Long historyId) { this.historyId = historyId; }
    }
}