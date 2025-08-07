package com.example;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * SpringBoot数据库配置系统主启动类
 * 
 * 本系统实现了将所有应用配置（包括数据源、缓存、消息队列、业务参数、框架配置等）
 * 都存储在数据库中的动态配置管理方案。
 * 
 * 核心特性：
 * 1. 动态配置管理 - 运行时修改配置无需重启
 * 2. 配置加密存储 - 敏感信息自动加密保存
 * 3. 多环境支持 - 统一管理不同环境的配置
 * 4. 配置版本控制 - 完整的变更历史和回滚机制
 * 5. 框架级配置支持 - 包括日志级别等框架配置的动态修改
 * 6. 监控告警 - 配置变更的实时监控和告警
 * 
 * @author AI Assistant
 * @version 1.0.0
 */
@SpringBootApplication(exclude = {
    DataSourceAutoConfiguration.class,
    DataSourceTransactionManagerAutoConfiguration.class,
    HibernateJpaAutoConfiguration.class
})
@EnableConfigurationProperties
@EnableCaching
@EnableAsync
@EnableScheduling
@Slf4j
public class SpringbootDbCfgApplication {

    public static void main(String[] args) {
        try {
            log.info("========================================");
            log.info("启动SpringBoot数据库配置系统...");
            log.info("========================================");
            
            // 打印系统环境信息
            printSystemInfo();
            
            // 启动Spring Boot应用
            ConfigurableApplicationContext context = SpringApplication.run(SpringbootDbCfgApplication.class, args);
            
            // 打印启动成功信息
            printStartupInfo(context);
            
        } catch (Exception e) {
            log.error("应用启动失败", e);
            System.exit(1);
        }
    }
    
    /**
     * 打印系统环境信息
     */
    private static void printSystemInfo() {
        log.info("Java版本: {}", System.getProperty("java.version"));
        log.info("Spring Boot版本: {}", SpringBootApplication.class.getPackage().getImplementationVersion());
        log.info("操作系统: {} {}", System.getProperty("os.name"), System.getProperty("os.version"));
        log.info("运行环境: {}", System.getProperty("spring.profiles.active", "development"));
        log.info("工作目录: {}", System.getProperty("user.dir"));
        log.info("配置数据库主机: {}", System.getProperty("CONFIG_DB_HOST", "localhost"));
        log.info("配置数据库名称: {}", System.getProperty("CONFIG_DB_NAME", "app_config"));
    }
    
    /**
     * 打印启动成功信息
     */
    private static void printStartupInfo(ConfigurableApplicationContext context) {
        String port = context.getEnvironment().getProperty("server.port", "8080");
        String contextPath = context.getEnvironment().getProperty("server.servlet.context-path", "");
        
        log.info("========================================");
        log.info("🎉 SpringBoot数据库配置系统启动成功！");
        log.info("========================================");
        log.info("📍 应用访问地址:");
        log.info("   本地访问: http://localhost:{}{}", port, contextPath);
        log.info("   健康检查: http://localhost:{}{}/api/config/health", port, contextPath);
        log.info("   配置查看: http://localhost:{}{}/api/config/current", port, contextPath);
        log.info("   监控端点: http://localhost:{}{}/actuator", port, contextPath);
        log.info("========================================");
        log.info("💡 主要功能:");
        log.info("   ✅ 动态配置管理 - 运行时修改配置");
        log.info("   ✅ 配置加密存储 - 敏感信息安全保护");
        log.info("   ✅ 多环境支持 - 统一配置管理");
        log.info("   ✅ 版本控制 - 配置变更历史和回滚");
        log.info("   ✅ 框架级配置 - 日志级别动态调整");
        log.info("   ✅ 监控告警 - 实时配置变更监控");
        log.info("========================================");
        log.info("🔧 常用API:");
        log.info("   GET  /api/config/current - 获取当前环境所有配置");
        log.info("   GET  /api/config/{{type}}/{{env}} - 获取指定类型配置");
        log.info("   POST /api/config/business - 更新业务配置");
        log.info("   POST /api/config/framework - 更新框架配置");
        log.info("   GET  /api/config/history/{{key}} - 获取配置变更历史");
        log.info("   POST /api/config/rollback - 配置回滚");
        log.info("========================================");
        log.info("📊 系统状态:");
        log.info("   活跃Bean数量: {}", context.getBeanDefinitionCount());
        log.info("   内存使用: {} MB", 
                Runtime.getRuntime().totalMemory() / 1024 / 1024);
        log.info("   可用内存: {} MB", 
                Runtime.getRuntime().freeMemory() / 1024 / 1024);
        log.info("========================================");
        log.info("🎯 Ready for dynamic configuration management!");
        log.info("========================================");
    }
}