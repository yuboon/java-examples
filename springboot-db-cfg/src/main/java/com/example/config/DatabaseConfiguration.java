package com.example.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import javax.sql.DataSource;

@Configuration
@Slf4j
public class DatabaseConfiguration {

    @Autowired
    private Environment environment;

    /**
     * 配置数据库数据源 - 用于读取配置信息
     */
    @Bean(name = "configDataSource")
    public DataSource configDataSource() {
        HikariConfig config = new HikariConfig();
        
        // 从环境变量或默认值获取配置
        String username = environment.getProperty("spring.config-datasource.username", "root");
        String password = environment.getProperty("spring.config-datasource.password", "root");
        // 构建JDBC URL
        String jdbcUrl = environment.getProperty("spring.config-datasource.url");
        // 设置HikariCP配置
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");
        
        // 连接池配置
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30000);
        config.setIdleTimeout(600000);
        config.setMaxLifetime(1800000);
        config.setPoolName("ConfigDataSourcePool");
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");
        
        return new HikariDataSource(config);
    }

    /**
     * 业务数据源 - 作为默认数据源，从数据库配置中动态加载参数
     * 通过EarlyDatabaseConfigInitializer已经将数据库配置加载到Environment中
     */
    @Bean(name = "businessDataSource")
    @Primary
    public DataSource businessDataSource() {
        HikariConfig config = new HikariConfig();
        
        try {
            // 优先从数据库加载的配置中获取（通过EarlyDatabaseConfigInitializer加载）
            // 这些配置已经在早期阶段从数据库加载到Environment中
            String url = environment.getProperty("spring.datasource.url");
            String username = environment.getProperty("spring.datasource.username");  
            String password = environment.getProperty("spring.datasource.password");
            String driverClassName = environment.getProperty("spring.datasource.driver-class-name");
            
            // 如果数据库中没有配置，则使用默认值
            if (url == null) {
                url = "jdbc:mysql://localhost:3306/business_db?useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Shanghai";
                log.warn("未找到数据库配置的URL，使用默认值: {}", url);
            }
            if (username == null) {
                username = "root";
                log.warn("未找到数据库配置的用户名，使用默认值: {}", username);
            }
            if (password == null) {
                password = "password";
                log.warn("未找到数据库配置的密码，使用默认值");
            }
            if (driverClassName == null) {
                driverClassName = "com.mysql.cj.jdbc.Driver";
            }
            
            // 设置HikariCP基本配置
            config.setJdbcUrl(url);
            config.setUsername(username);
            config.setPassword(password);
            config.setDriverClassName(driverClassName);
            
            // 连接池配置 - 也支持从数据库配置中读取
            config.setMaximumPoolSize(environment.getProperty("spring.datasource.hikari.maximum-pool-size", Integer.class, 20));
            config.setMinimumIdle(environment.getProperty("spring.datasource.hikari.minimum-idle", Integer.class, 5));
            config.setConnectionTimeout(environment.getProperty("spring.datasource.hikari.connection-timeout", Long.class, 30000L));
            config.setIdleTimeout(environment.getProperty("spring.datasource.hikari.idle-timeout", Long.class, 600000L));
            config.setMaxLifetime(environment.getProperty("spring.datasource.hikari.max-lifetime", Long.class, 1800000L));
            config.setValidationTimeout(environment.getProperty("spring.datasource.hikari.validation-timeout", Long.class, 5000L));
            
            // 连接池名称和其他配置
            config.setPoolName("BusinessHikariCP");
            config.setAutoCommit(true);
            config.setConnectionTestQuery("SELECT 1");
            
            log.info("创建业务数据源 - URL: {}, 用户: {}, 连接池大小: {}", 
                    url, username, config.getMaximumPoolSize());
            
            return new HikariDataSource(config);
            
        } catch (Exception e) {
            log.error("创建业务数据源失败", e);
            throw new RuntimeException("业务数据源创建失败", e);
        }
    }

    /**
     * 配置JdbcTemplate - 用于读取配置信息
     */
    @Bean(name = "configJdbcTemplate")
    public JdbcTemplate configJdbcTemplate(@Qualifier("configDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /**
     * 默认JdbcTemplate - 使用业务数据源
     */
    @Bean(name = "jdbcTemplate")
    @Primary
    public JdbcTemplate jdbcTemplate(@Qualifier("businessDataSource") DataSource businessDataSource) {
        return new JdbcTemplate(businessDataSource);
    }

    /**
     * 配置事务管理器
     */
    @Bean(name = "configTransactionManager")
    public PlatformTransactionManager configTransactionManager(
            @Qualifier("configDataSource") DataSource dataSource) {
        return new DataSourceTransactionManager(dataSource);
    }

    /**
     * 默认事务管理器 - 使用业务数据源
     */
    @Bean(name = "transactionManager")
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("businessDataSource") DataSource businessDataSource) {
        return new DataSourceTransactionManager(businessDataSource);
    }
}