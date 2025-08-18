package com.example.sqltree;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * SQL调用树配置属性
 */
@Data
@Component
@ConfigurationProperties(prefix = "sql-tree")
public class SqlTreeProperties {
    
    /**
     * 慢SQL阈值(毫秒)
     */
    private long slowSqlThreshold = 1000L;
    
    /**
     * 是否启用SQL追踪
     */
    private boolean traceEnabled = true;
    
    /**
     * 最大调用深度
     */
    private int maxDepth = 50;
    
    /**
     * 最大会话数量
     */
    private int maxSessions = 100;
    
    /**
     * 是否记录SQL参数
     */
    private boolean recordParameters = true;
}