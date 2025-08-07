package com.example.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import org.springframework.boot.logging.LogLevel;
import org.springframework.boot.logging.LoggingSystem;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 动态日志级别管理服务
 * 支持运行时修改日志级别，无需重启应用
 */
@Service
@Slf4j
public class DynamicLoggingService {

    private final LoggingSystem loggingSystem;
    private final LoggerContext loggerContext;

    public DynamicLoggingService() {
        this.loggingSystem = LoggingSystem.get(this.getClass().getClassLoader());
        this.loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
    }

    @PostConstruct
    public void init() {
        log.info("动态日志级别管理服务初始化完成");
        log.debug("当前日志上下文: {}", loggerContext.getName());
    }

    /**
     * 动态设置日志级别
     * @param loggerName 日志器名称（包名或类名）
     * @param level 日志级别
     */
    public void setLogLevel(String loggerName, String level) {
        try {
            LogLevel logLevel = parseLogLevel(level);
            
            // 使用Spring Boot的LoggingSystem设置日志级别
            loggingSystem.setLogLevel(loggerName, logLevel);
            
            log.info("成功设置日志级别: {} = {}", loggerName, level);
            
        } catch (Exception e) {
            log.error("设置日志级别失败: {} = {}", loggerName, level, e);
            throw new RuntimeException("设置日志级别失败", e);
        }
    }

    /**
     * 获取当前日志级别
     * @param loggerName 日志器名称
     * @return 日志级别
     */
    public String getLogLevel(String loggerName) {
        try {
            Logger logger = loggerContext.getLogger(loggerName);
            Level level = logger.getLevel();
            
            if (level != null) {
                return level.toString();
            } else {
                // 如果当前logger没有设置级别，获取有效级别
                Level effectiveLevel = logger.getEffectiveLevel();
                return effectiveLevel != null ? effectiveLevel.toString() : "INHERITED";
            }
        } catch (Exception e) {
            log.error("获取日志级别失败: {}", loggerName, e);
            return "UNKNOWN";
        }
    }

    /**
     * 获取所有已配置的日志器及其级别
     */
    public Map<String, String> getAllLogLevels() {
        Map<String, String> logLevels = new HashMap<>();
        
        try {
            for (Logger logger : loggerContext.getLoggerList()) {
                if (logger.getLevel() != null) {
                    logLevels.put(logger.getName(), logger.getLevel().toString());
                }
            }
            
            // 添加根日志器
            Logger rootLogger = loggerContext.getLogger(Logger.ROOT_LOGGER_NAME);
            if (rootLogger.getLevel() != null) {
                logLevels.put("ROOT", rootLogger.getLevel().toString());
            }
            
        } catch (Exception e) {
            log.error("获取所有日志级别失败", e);
        }
        
        return logLevels;
    }

    /**
     * 批量设置日志级别
     * @param logLevels 日志级别映射
     */
    public void setLogLevels(Map<String, String> logLevels) {
        for (Map.Entry<String, String> entry : logLevels.entrySet()) {
            try {
                setLogLevel(entry.getKey(), entry.getValue());
            } catch (Exception e) {
                log.error("批量设置日志级别失败: {} = {}", entry.getKey(), entry.getValue(), e);
            }
        }
        log.info("批量设置日志级别完成，共处理 {} 个配置", logLevels.size());
    }

    /**
     * 重置日志级别为默认值
     * @param loggerName 日志器名称
     */
    public void resetLogLevel(String loggerName) {
        try {
            Logger logger = loggerContext.getLogger(loggerName);
            logger.setLevel(null); // 设置为null会继承父级别
            log.info("成功重置日志级别: {}", loggerName);
        } catch (Exception e) {
            log.error("重置日志级别失败: {}", loggerName, e);
            throw new RuntimeException("重置日志级别失败", e);
        }
    }

    /**
     * 测试日志输出
     * @param loggerName 日志器名称
     */
    public Map<String, Object> testLogOutput(String loggerName) {
        try {
            Logger testLogger = loggerContext.getLogger(loggerName);
            String currentLevel = getLogLevel(loggerName);
            String effectiveLevel = testLogger.getEffectiveLevel().toString();
            
            // 输出测试日志
            testLogger.trace("TRACE 级别测试日志");
            testLogger.debug("DEBUG 级别测试日志");
            testLogger.info("INFO 级别测试日志");
            testLogger.warn("WARN 级别测试日志");
            testLogger.error("ERROR 级别测试日志");
            
            return Map.of(
                "loggerName", loggerName,
                "currentLevel", currentLevel,
                "effectiveLevel", effectiveLevel,
                "message", "日志测试完成，请查看控制台输出"
            );
            
        } catch (Exception e) {
            log.error("测试日志输出失败: {}", loggerName, e);
            return Map.of(
                "error", e.getMessage(),
                "loggerName", loggerName
            );
        }
    }

    /**
     * 解析日志级别字符串
     */
    private LogLevel parseLogLevel(String level) {
        try {
            return LogLevel.valueOf(level.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            // 如果解析失败，尝试兼容性处理
            switch (level.trim().toLowerCase()) {
                case "off":
                    return LogLevel.OFF;
                case "error":
                    return LogLevel.ERROR;
                case "warn":
                case "warning":
                    return LogLevel.WARN;
                case "info":
                    return LogLevel.INFO;
                case "debug":
                    return LogLevel.DEBUG;
                case "trace":
                    return LogLevel.TRACE;
                default:
                    throw new IllegalArgumentException("不支持的日志级别: " + level);
            }
        }
    }

    /**
     * 获取支持的日志级别列表
     */
    public String[] getSupportedLogLevels() {
        return new String[]{"TRACE", "DEBUG", "INFO", "WARN", "ERROR", "OFF"};
    }
}