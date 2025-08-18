package com.example.sqltree;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * SQL调用树REST API控制器
 * 提供SQL调用树的查询、管理和配置接口
 */
@Slf4j
@RestController
@RequestMapping("/api/sql-tree")
@CrossOrigin(origins = "*", maxAge = 3600)
public class SqlTreeController {
    
    @Autowired
    private SqlCallTreeContext sqlCallTreeContext;
    
    /**
     * 获取当前线程的SQL调用树
     * @param limit 限制返回的数据量
     * @param sort 排序方式：latest(最新) 或 slowest(最慢)
     * @return API响应
     */
    @GetMapping("/current")
    public ResponseEntity<ApiResponse<List<SqlNode>>> getCurrentThreadTree(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort) {
        try {
            List<SqlNode> rootNodes = sqlCallTreeContext.getRootNodes();
            rootNodes = applyDataLimits(rootNodes, limit, sort);
            return ResponseEntity.ok(ApiResponse.success(rootNodes));
        } catch (Exception e) {
            log.error("获取当前线程SQL调用树失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取当前线程SQL调用树失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有会话的SQL调用树
     * @param limit 限制返回的数据量
     * @param sort 排序方式：latest(最新) 或 slowest(最慢)
     * @return API响应
     */
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<Map<String, List<SqlNode>>>> getAllSessions(
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort) {
        try {
            Map<String, List<SqlNode>> sessions = sqlCallTreeContext.getAllSessions();
            // 对每个会话的数据应用限制
            for (Map.Entry<String, List<SqlNode>> entry : sessions.entrySet()) {
                entry.setValue(applyDataLimits(entry.getValue(), limit, sort));
            }
            return ResponseEntity.ok(ApiResponse.success(sessions));
        } catch (Exception e) {
            log.error("获取所有会话SQL调用树失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取所有会话SQL调用树失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取所有线程ID列表
     * @return API响应
     */
    @GetMapping("/thread-ids")
    public ResponseEntity<ApiResponse<List<String>>> getAllThreadIds() {
        try {
            List<String> threadIds = sqlCallTreeContext.getAllThreadIds();
            return ResponseEntity.ok(ApiResponse.success(threadIds));
        } catch (Exception e) {
            log.error("获取线程ID列表失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取线程ID列表失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取指定线程的SQL调用树
     * @param threadId 线程ID
     * @param limit 限制返回的数据量
     * @param sort 排序方式：latest(最新) 或 slowest(最慢)
     * @return API响应
     */
    @GetMapping("/threads/{threadId}")
    public ResponseEntity<ApiResponse<List<SqlNode>>> getThreadTree(
            @PathVariable String threadId,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) String sort) {
        try {
            List<SqlNode> rootNodes = sqlCallTreeContext.getSessionTree(threadId);
            rootNodes = applyDataLimits(rootNodes, limit, sort);
            return ResponseEntity.ok(ApiResponse.success(rootNodes));
        } catch (Exception e) {
            log.error("获取线程SQL调用树失败: threadId={}", threadId, e);
            return ResponseEntity.ok(ApiResponse.error("获取线程SQL调用树失败: " + e.getMessage()));
        }
    }
    
    /**
     * 清空当前线程的SQL调用树
     * @return API响应
     */
    @DeleteMapping("/current")
    public ResponseEntity<ApiResponse<String>> clearCurrentThreadTree() {
        try {
            sqlCallTreeContext.clear();
            return ResponseEntity.ok(ApiResponse.success("当前线程SQL调用树已清空"));
        } catch (Exception e) {
            log.error("清空当前线程SQL调用树失败", e);
            return ResponseEntity.ok(ApiResponse.error("清空当前线程SQL调用树失败: " + e.getMessage()));
        }
    }
    
    /**
     * 清空所有会话的SQL调用树
     * @return API响应
     */
    @DeleteMapping("/sessions")
    public ResponseEntity<ApiResponse<String>> clearAllSessions() {
        try {
            sqlCallTreeContext.clearAll();
            return ResponseEntity.ok(ApiResponse.success("所有会话SQL调用树已清空"));
        } catch (Exception e) {
            log.error("清空所有会话SQL调用树失败", e);
            return ResponseEntity.ok(ApiResponse.error("清空所有会话SQL调用树失败: " + e.getMessage()));
        }
    }

    /**
     * 清空当前线程的SQL调用树（POST方式）
     * @return API响应
     */
    @PostMapping("/clear")
    public ResponseEntity<ApiResponse<String>> clearCurrentThread() {
        try {
            sqlCallTreeContext.clear();
            return ResponseEntity.ok(ApiResponse.success("当前线程SQL调用树已清空"));
        } catch (Exception e) {
            log.error("清空当前线程SQL调用树失败", e);
            return ResponseEntity.ok(ApiResponse.error("清空当前线程SQL调用树失败: " + e.getMessage()));
        }
    }

    /**
     * 清空所有会话的SQL调用树（POST方式）
     * @return API响应
     */
    @PostMapping("/clear-all")
    public ResponseEntity<ApiResponse<String>> clearAllThreads() {
        try {
            sqlCallTreeContext.clearAll();
            return ResponseEntity.ok(ApiResponse.success("所有会话SQL调用树已清空"));
        } catch (Exception e) {
            log.error("清空所有会话SQL调用树失败", e);
            return ResponseEntity.ok(ApiResponse.error("清空所有会话SQL调用树失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取统计信息
     * @return API响应
     */
    @GetMapping("/statistics")
    public ResponseEntity<ApiResponse<SqlCallTreeContext.SqlTraceStatistics>> getStatistics() {
        try {
            SqlCallTreeContext.SqlTraceStatistics statistics = sqlCallTreeContext.getStatistics();
            return ResponseEntity.ok(ApiResponse.success(statistics));
        } catch (Exception e) {
            log.error("获取统计信息失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取统计信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 重置统计信息
     * @return API响应
     */
    @PostMapping("/statistics/reset")
    public ResponseEntity<ApiResponse<String>> resetStatistics() {
        try {
            sqlCallTreeContext.getStatistics().reset();
            return ResponseEntity.ok(ApiResponse.success("统计信息已重置"));
        } catch (Exception e) {
            log.error("重置统计信息失败", e);
            return ResponseEntity.ok(ApiResponse.error("重置统计信息失败: " + e.getMessage()));
        }
    }
    
    /**
     * 设置追踪配置
     * @param config 追踪配置
     * @return API响应
     */
    @PostMapping("/config")
    public ResponseEntity<ApiResponse<String>> setTraceConfig(@RequestBody SqlCallTreeContext.SqlTraceConfig config) {
        try {
            sqlCallTreeContext.setTraceConfig(config);
            if (config.getSlowSqlThreshold() > 0) {
                sqlCallTreeContext.setSlowSqlThreshold(config.getSlowSqlThreshold());
            }
            sqlCallTreeContext.setTraceEnabled(config.isEnabled());
            return ResponseEntity.ok(ApiResponse.success("追踪配置已更新"));
        } catch (Exception e) {
            log.error("设置追踪配置失败", e);
            return ResponseEntity.ok(ApiResponse.error("设置追踪配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取追踪配置
     * @return API响应
     */
    @GetMapping("/config")
    public ResponseEntity<ApiResponse<SqlCallTreeContext.SqlTraceConfig>> getTraceConfig() {
        try {
            SqlCallTreeContext.SqlTraceConfig config = sqlCallTreeContext.getTraceConfig();
            return ResponseEntity.ok(ApiResponse.success(config));
        } catch (Exception e) {
            log.error("获取追踪配置失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取追踪配置失败: " + e.getMessage()));
        }
    }
    
    /**
     * 分析慢SQL
     * @param threshold 慢SQL阈值(毫秒)
     * @return API响应
     */
    @GetMapping("/slow-sql")
    public ResponseEntity<ApiResponse<SlowSqlAnalysis>> analyzeSlowSql(
            @RequestParam(defaultValue = "1000") long threshold) {
        try {
            SlowSqlAnalysis analysis = new SlowSqlAnalysis();
            analysis.setThreshold(threshold);
            
            Map<String, List<SqlNode>> sessions = sqlCallTreeContext.getAllSessions();
            for (Map.Entry<String, List<SqlNode>> entry : sessions.entrySet()) {
                String sessionId = entry.getKey();
                List<SqlNode> rootNodes = entry.getValue();
                
                for (SqlNode rootNode : rootNodes) {
                    analyzeNodeForSlowSql(rootNode, threshold, analysis, sessionId);
                }
            }
            
            return ResponseEntity.ok(ApiResponse.success(analysis));
        } catch (Exception e) {
            log.error("分析慢SQL失败", e);
            return ResponseEntity.ok(ApiResponse.error("分析慢SQL失败: " + e.getMessage()));
        }
    }
    
    /**
     * 导出SQL调用树数据
     * @return API响应
     */
    @GetMapping("/export")
    public ResponseEntity<ApiResponse<SqlTreeExportData>> exportData() {
        try {
            SqlTreeExportData exportData = new SqlTreeExportData();
            exportData.setSessions(sqlCallTreeContext.getAllSessions());
            exportData.setStatistics(sqlCallTreeContext.getStatistics());
            exportData.setConfig(sqlCallTreeContext.getTraceConfig());
            exportData.setExportTime(LocalDateTime.now());
            exportData.setSlowSqlThreshold(sqlCallTreeContext.getSlowSqlThreshold());
            
            return ResponseEntity.ok(ApiResponse.success(exportData));
        } catch (Exception e) {
            log.error("导出SQL调用树数据失败", e);
            return ResponseEntity.ok(ApiResponse.error("导出SQL调用树数据失败: " + e.getMessage()));
        }
    }
    
    /**
     * 获取系统状态
     * @return API响应
     */
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<SystemStatus>> getSystemStatus() {
        try {
            SystemStatus status = new SystemStatus();
            status.setTraceEnabled(sqlCallTreeContext.isTraceEnabled());
            status.setSlowSqlThreshold(sqlCallTreeContext.getSlowSqlThreshold());
            status.setCurrentDepth(sqlCallTreeContext.getCurrentDepth());
            status.setSessionCount(sqlCallTreeContext.getAllSessions().size());
            status.setStatistics(sqlCallTreeContext.getStatistics());
            
            return ResponseEntity.ok(ApiResponse.success(status));
        } catch (Exception e) {
            log.error("获取系统状态失败", e);
            return ResponseEntity.ok(ApiResponse.error("获取系统状态失败: " + e.getMessage()));
        }
    }
    
    /**
     * 应用数据量限制和排序
     * @param nodes 原始节点列表
     * @param limit 限制数量
     * @param sort 排序方式
     * @return 处理后的节点列表
     */
    private List<SqlNode> applyDataLimits(List<SqlNode> nodes, Integer limit, String sort) {
        if (nodes == null || nodes.isEmpty()) {
            return nodes;
        }
        
        List<SqlNode> result = new java.util.ArrayList<>(nodes);
        
        // 应用排序
        if ("slowest".equals(sort)) {
            result.sort((a, b) -> Long.compare(b.getExecutionTime(), a.getExecutionTime()));
        } else if ("latest".equals(sort)) {
            result.sort((a, b) -> {
                if (a.getStartTime() == null && b.getStartTime() == null) return 0;
                if (a.getStartTime() == null) return 1;
                if (b.getStartTime() == null) return -1;
                return b.getStartTime().compareTo(a.getStartTime());
            });
        }
        
        // 应用数量限制
        if (limit != null && limit > 0 && result.size() > limit) {
            result = result.subList(0, limit);
        }
        
        return result;
    }
    
    /**
     * 递归分析节点中的慢SQL
     * @param node 节点
     * @param threshold 阈值
     * @param analysis 分析结果
     * @param sessionId 会话ID
     */
    private void analyzeNodeForSlowSql(SqlNode node, long threshold, SlowSqlAnalysis analysis, String sessionId) {
        if (node.isSlowSql(threshold)) {
            SlowSqlInfo slowSqlInfo = new SlowSqlInfo();
            slowSqlInfo.setSessionId(sessionId);
            slowSqlInfo.setNodeId(node.getNodeId());
            slowSqlInfo.setSql(node.getSql());
            slowSqlInfo.setExecutionTime(node.getExecutionTime());
            slowSqlInfo.setDepth(node.getDepth());
            slowSqlInfo.setStartTime(node.getStartTime());
            slowSqlInfo.setThreadName(node.getThreadName());
            
            analysis.getSlowSqlList().add(slowSqlInfo);
            analysis.setTotalSlowSqlCount(analysis.getTotalSlowSqlCount() + 1);
            analysis.setTotalSlowSqlTime(analysis.getTotalSlowSqlTime() + node.getExecutionTime());
        }
        
        // 递归分析子节点
        for (SqlNode child : node.getChildren()) {
            analyzeNodeForSlowSql(child, threshold, analysis, sessionId);
        }
    }
    
    /**
     * 通用API响应包装类
     * @param <T> 数据类型
     */
    @Data
    public static class ApiResponse<T> {
        private boolean success;
        private String message;
        private T data;
        private LocalDateTime timestamp;
        
        public ApiResponse() {
            this.timestamp = LocalDateTime.now();
        }
        
        public static <T> ApiResponse<T> success(T data) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(true);
            response.setMessage("操作成功");
            response.setData(data);
            return response;
        }
        
        public static <T> ApiResponse<T> error(String message) {
            ApiResponse<T> response = new ApiResponse<>();
            response.setSuccess(false);
            response.setMessage(message);
            return response;
        }
    }
    
    /**
     * SQL调用树导出数据
     */
    @Data
    public static class SqlTreeExportData {
        private Map<String, List<SqlNode>> sessions;
        private SqlCallTreeContext.SqlTraceStatistics statistics;
        private SqlCallTreeContext.SqlTraceConfig config;
        private LocalDateTime exportTime;
        private long slowSqlThreshold;
    }
    
    /**
     * 慢SQL分析结果
     */
    @Data
    public static class SlowSqlAnalysis {
        private long threshold;
        private int totalSlowSqlCount = 0;
        private long totalSlowSqlTime = 0;
        private List<SlowSqlInfo> slowSqlList = new java.util.ArrayList<>();
        
        public double getAverageSlowSqlTime() {
            return totalSlowSqlCount > 0 ? (double) totalSlowSqlTime / totalSlowSqlCount : 0.0;
        }
    }
    
    /**
     * 慢SQL信息
     */
    @Data
    public static class SlowSqlInfo {
        private String sessionId;
        private String nodeId;
        private String sql;
        private long executionTime;
        private int depth;
        private LocalDateTime startTime;
        private String threadName;
    }
    
    /**
     * 系统状态
     */
    @Data
    public static class SystemStatus {
        private boolean traceEnabled;
        private long slowSqlThreshold;
        private int currentDepth;
        private int sessionCount;
        private SqlCallTreeContext.SqlTraceStatistics statistics;
    }
}