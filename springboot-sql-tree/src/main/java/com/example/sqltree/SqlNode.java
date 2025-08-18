package com.example.sqltree;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * SQL调用节点数据模型
 * 用于构建SQL调用树的基本数据结构
 */
@Data
public class SqlNode {
    
    /**
     * 节点唯一标识
     */
    private String nodeId;
    
    /**
     * SQL语句
     */
    private String sql;
    
    /**
     * 格式化后的SQL语句
     */
    private String formattedSql;
    
    /**
     * SQL类型 (SELECT, INSERT, UPDATE, DELETE)
     */
    private String sqlType;
    
    /**
     * 调用深度
     */
    private int depth;
    
    /**
     * 线程名称
     */
    private String threadName;
    
    /**
     * Service类名
     */
    private String serviceName;
    
    /**
     * Service方法名
     */
    private String methodName;
    
    /**
     * Service调用路径
     */
    private String serviceCallPath;
    
    /**
     * 开始时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss.SSS")
    private LocalDateTime endTime;
    
    /**
     * 执行耗时(毫秒)
     */
    private long executionTime;
    
    /**
     * 是否为慢SQL
     */
    private boolean slowSql;
    
    /**
     * 影响行数
     */
    private int affectedRows;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * SQL参数
     */
    private List<Object> parameters;
    
    /**
     * 子节点列表
     */
    private List<SqlNode> children;
    
    /**
     * 父节点ID
     */
    private String parentId;
    
    /**
     * 构造函数
     */
    public SqlNode() {
        this.nodeId = UUID.randomUUID().toString();
        this.children = new ArrayList<>();
        this.parameters = new ArrayList<>();
        this.startTime = LocalDateTime.now();
        this.threadName = Thread.currentThread().getName();
    }
    
    /**
     * 构造函数
     * @param sql SQL语句
     * @param sqlType SQL类型
     * @param depth 调用深度
     */
    public SqlNode(String sql, String sqlType, int depth) {
        this();
        this.sql = sql;
        this.sqlType = sqlType;
        this.depth = depth;
        this.formattedSql = formatSql(sql);
    }
    
    /**
     * 添加子节点
     * @param child 子节点
     */
    public void addChild(SqlNode child) {
        if (child != null) {
            child.setParentId(this.nodeId);
            this.children.add(child);
        }
    }
    
    /**
     * 获取子节点列表
     * @return 子节点列表
     */
    public List<SqlNode> getChildren() {
        return this.children;
    }
    
    /**
     * 设置结束时间并计算执行耗时
     */
    public void setEndTime() {
        this.endTime = LocalDateTime.now();
        this.executionTime = calculateExecutionTime();
    }
    
    /**
     * 设置结束时间并计算执行耗时
     * @param endTime 结束时间
     */
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
        this.executionTime = calculateExecutionTime();
    }
    
    /**
     * 计算执行耗时
     * @return 执行耗时(毫秒)
     */
    private long calculateExecutionTime() {
        if (startTime != null && endTime != null) {
            return java.time.Duration.between(startTime, endTime).toMillis();
        }
        return 0;
    }
    
    /**
     * 格式化SQL语句
     * @param sql 原始SQL
     * @return 格式化后的SQL
     */
    private String formatSql(String sql) {
        if (sql == null || sql.trim().isEmpty()) {
            return sql;
        }
        
        // 简单的SQL格式化
        return sql.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("\\s*,\\s*", ", ")
                .replaceAll("\\s*(=|>|<|>=|<=|!=)\\s*", " $1 ")
                .replaceAll("\\s+(AND|OR|WHERE|FROM|JOIN|LEFT|RIGHT|INNER|OUTER|ON|GROUP|ORDER|HAVING|LIMIT)\\s+", " $1 ")
                .replaceAll("\\s+(BY|ASC|DESC)\\s+", " $1 ");
    }
    
    /**
     * 判断是否为慢SQL
     * @param threshold 慢SQL阈值(毫秒)
     * @return 是否为慢SQL
     */
    public boolean isSlowSql(long threshold) {
        return this.executionTime > threshold;
    }
    
    /**
     * 设置慢SQL标记
     * @param threshold 慢SQL阈值(毫秒)
     */
    public void markSlowSql(long threshold) {
        this.slowSql = isSlowSql(threshold);
    }
    
    /**
     * 获取慢SQL标记（用于JSON序列化）
     * @return 是否为慢SQL
     */
    public boolean isSlowSql() {
        return this.slowSql;
    }
    
    /**
     * 获取慢SQL标记（用于JSON序列化）
     * @return 是否为慢SQL
     */
    public boolean getSlowSql() {
        return this.slowSql;
    }
    
    /**
     * 获取节点总数(包括子节点)
     * @return 节点总数
     */
    public int getTotalNodeCount() {
        int count = 1; // 当前节点
        for (SqlNode child : children) {
            count += child.getTotalNodeCount();
        }
        return count;
    }
    
    /**
     * 获取最大深度
     * @return 最大深度
     */
    public int getMaxDepth() {
        int maxDepth = this.depth;
        for (SqlNode child : children) {
            maxDepth = Math.max(maxDepth, child.getMaxDepth());
        }
        return maxDepth;
    }
    
    /**
     * 获取慢SQL节点数量
     * @return 慢SQL节点数量
     */
    public int getSlowSqlCount() {
        int count = this.slowSql ? 1 : 0;
        for (SqlNode child : children) {
            count += child.getSlowSqlCount();
        }
        return count;
    }
    
    /**
     * 获取总执行时间(包括子节点)
     * @return 总执行时间(毫秒)
     */
    public long getTotalExecutionTime() {
        long totalTime = this.executionTime;
        for (SqlNode child : children) {
            totalTime += child.getTotalExecutionTime();
        }
        return totalTime;
    }
}