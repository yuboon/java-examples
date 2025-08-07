package com.example.service;

import com.example.entity.ApplicationConfig;
import com.example.repository.ApplicationConfigRepository;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
public class DynamicDataSourceService {

    @Autowired
    private ApplicationConfigRepository configRepository;
    
    @Autowired
    private ConfigEncryptionService encryptionService;
    
    @Autowired
    private ApplicationContext applicationContext;
    
    @Autowired
    private ConfigurableEnvironment environment;

    /**
     * 从数据库配置创建动态数据源
     */
    public void createDynamicDataSources() {
        String activeEnvironment = environment.getProperty("environment.active", "development");
        log.info("开始创建动态数据源，当前环境: {}", activeEnvironment);

        try {
            // 获取数据源配置
            List<ApplicationConfig> datasourceConfigs = configRepository
                    .findByConfigTypeAndEnvironmentAndActiveTrue("datasource", activeEnvironment);

            log.info("从数据库查询到 {} 条数据源配置", datasourceConfigs.size());
            
            if (datasourceConfigs.isEmpty()) {
                log.warn("数据库中没有找到环境 {} 的数据源配置", activeEnvironment);
                return;
            }

            // 打印所有找到的配置
            datasourceConfigs.forEach(config -> 
                log.debug("数据源配置: {} = {} (加密: {})", 
                    config.getConfigKey(), 
                    config.getEncrypted() ? "***" : config.getConfigValue(),
                    config.getEncrypted()));

            // 将配置转换为Map
            Map<String, String> configMap = datasourceConfigs.stream()
                    .collect(Collectors.toMap(
                            ApplicationConfig::getConfigKey,
                            config -> getDecryptedValue(config)
                    ));

            log.info("数据源配置映射: {}", configMap.keySet());

            // 创建业务数据源
            createBusinessDataSource(configMap);
            
        } catch (Exception e) {
            log.error("创建动态数据源时发生异常", e);
            throw e;
        }
    }

    /**
     * 创建业务数据源
     */
    private void createBusinessDataSource(Map<String, String> configMap) {
        try {
            String url = configMap.get("url");
            String username = configMap.get("username");
            String password = configMap.get("password");
            String driverClassName = configMap.get("driver-class-name");

            log.info("准备创建业务数据源:");
            log.info("  URL: {}", url);
            log.info("  Username: {}", username);
            log.info("  Password: {}", password != null ? "***" : null);
            log.info("  Driver: {}", driverClassName);

            if (url == null || username == null || password == null) {
                log.error("业务数据源配置不完整: url={}, username={}, password={}", 
                         url, username, password != null ? "***" : null);
                return;
            }

            log.info("开始创建 HikariCP 数据源...");

            HikariConfig hikariConfig = new HikariConfig();
            hikariConfig.setJdbcUrl(url);
            hikariConfig.setUsername(username);
            hikariConfig.setPassword(password);
            hikariConfig.setDriverClassName(driverClassName != null ? driverClassName : "com.mysql.cj.jdbc.Driver");

            // 设置连接池参数
            int maxPoolSize = parseInt(configMap.get("maximum-pool-size"), 20);
            int minIdle = parseInt(configMap.get("minimum-idle"), 5);
            int connectionTimeout = parseInt(configMap.get("connection-timeout"), 30000);
            int idleTimeout = parseInt(configMap.get("idle-timeout"), 600000);
            int maxLifetime = parseInt(configMap.get("max-lifetime"), 1800000);
            
            hikariConfig.setMaximumPoolSize(maxPoolSize);
            hikariConfig.setMinimumIdle(minIdle);
            hikariConfig.setConnectionTimeout(connectionTimeout);
            hikariConfig.setIdleTimeout(idleTimeout);
            hikariConfig.setMaxLifetime(maxLifetime);
            hikariConfig.setPoolName("BusinessDataSourcePool");
            hikariConfig.setAutoCommit(true);
            hikariConfig.setConnectionTestQuery("SELECT 1");

            log.info("HikariCP 配置参数:");
            log.info("  最大连接池大小: {}", maxPoolSize);
            log.info("  最小空闲连接: {}", minIdle);
            log.info("  连接超时: {}ms", connectionTimeout);

            HikariDataSource businessDataSource = new HikariDataSource(hikariConfig);
            log.info("HikariCP 数据源创建成功");

            // 测试连接
            log.info("开始测试数据源连接...");
            try (var connection = businessDataSource.getConnection()) {
                boolean isValid = connection.isValid(5);
                log.info("数据源连接测试结果: {}", isValid ? "成功" : "失败");
                
                if (isValid) {
                    log.info("注册业务数据源到Spring容器...");
                    registerBusinessDataSource(businessDataSource);
                    log.info("业务数据源注册成功: businessDataSource");
                } else {
                    log.error("业务数据源连接测试失败");
                    businessDataSource.close();
                }
            }

        } catch (Exception e) {
            log.error("创建业务数据源失败", e);
        }
    }

    /**
     * 注册业务数据源到Spring容器
     */
    private void registerBusinessDataSource(DataSource businessDataSource) {
        DefaultListableBeanFactory beanFactory = 
                (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();

        // 检查是否已存在业务数据源
        if (beanFactory.containsBean("businessDataSource")) {
            log.info("发现已存在的业务数据源，准备替换...");
            try {
                DataSource oldDataSource = beanFactory.getBean("businessDataSource", DataSource.class);
                if (oldDataSource instanceof HikariDataSource) {
                    log.info("关闭旧的业务数据源");
                    ((HikariDataSource) oldDataSource).close();
                }
                beanFactory.destroySingleton("businessDataSource");
                log.info("旧的业务数据源已移除");
            } catch (Exception e) {
                log.warn("关闭旧业务数据源时出现异常", e);
            }
        }

        // 注册新的业务数据源
        beanFactory.registerSingleton("businessDataSource", businessDataSource);
        log.info("新的业务数据源已成功注册到Spring容器: businessDataSource");
        
        // 验证注册是否成功
        if (beanFactory.containsBean("businessDataSource")) {
            log.info("验证: 业务数据源注册成功，可通过@Qualifier(\"businessDataSource\")注入使用");
        } else {
            log.error("验证: 业务数据源注册失败！");
        }
    }

    /**
     * 获取解密后的配置值
     */
    private String getDecryptedValue(ApplicationConfig config) {
        try {
            if (Boolean.TRUE.equals(config.getEncrypted())) {
                return encryptionService.decrypt(config.getConfigValue());
            } else {
                return config.getConfigValue();
            }
        } catch (Exception e) {
            log.error("解密配置失败: {}", config.getConfigKey(), e);
            return config.getConfigValue();
        }
    }

    /**
     * 安全的整数解析
     */
    private int parseInt(String value, int defaultValue) {
        if (value == null || value.trim().isEmpty()) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            log.warn("无法解析整数值: {}, 使用默认值: {}", value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * 动态更新业务数据源
     */
    public void updateBusinessDataSource() {
        log.info("手动更新业务数据源");
        createDynamicDataSources();
    }

    /**
     * 获取当前业务数据源状态
     */
    public boolean isBusinessDataSourceAvailable() {
        try {
            DefaultListableBeanFactory beanFactory = 
                    (DefaultListableBeanFactory) applicationContext.getAutowireCapableBeanFactory();
            
            if (!beanFactory.containsBean("businessDataSource")) {
                return false;
            }
            
            DataSource dataSource = beanFactory.getBean("businessDataSource", DataSource.class);
            try (var connection = dataSource.getConnection()) {
                return connection.isValid(5);
            }
        } catch (Exception e) {
            log.warn("检查业务数据源状态时出现异常", e);
            return false;
        }
    }
}