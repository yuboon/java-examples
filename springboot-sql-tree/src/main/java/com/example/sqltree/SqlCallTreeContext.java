package com.example.sqltree;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * SQL调用树上下文管理器
 * 负责管理SQL调用树的构建、存储和查询
 */
@Slf4j
@Component
public class SqlCallTreeContext {
    
    private final SqlTreeProperties sqlTreeProperties;
    
    /**
     * 线程本地存储 - SQL调用栈
     */
    private final ThreadLocal<Stack<SqlNode>> callStack = new ThreadLocal<Stack<SqlNode>>() {
        @Override
        protected Stack<SqlNode> initialValue() {
            return new Stack<>();
        }
    };
    
    /**
     * 线程本地存储 - Service调用栈
     */
    private final ThreadLocal<Stack<ServiceCallInfo>> serviceCallStack = new ThreadLocal<Stack<ServiceCallInfo>>() {
        @Override
        protected Stack<ServiceCallInfo> initialValue() {
            return new Stack<>();
        }
    };
    
    /**
     * 线程本地存储 - 根节点列表
     */
    private final ThreadLocal<List<SqlNode>> rootNodes = new ThreadLocal<List<SqlNode>>() {
        @Override
        protected List<SqlNode> initialValue() {
            return new ArrayList<>();
        }
    };
    
    /**
     * 线程本地存储 - 配置信息
     */
    private final ThreadLocal<SqlTraceConfig> traceConfig = new ThreadLocal<SqlTraceConfig>() {
        @Override
        protected SqlTraceConfig initialValue() {
            return new SqlTraceConfig();
        }
    };
    
    /**
     * 全局会话存储 - 线程ID -> 根节点列表
     */
    private final Map<String, List<SqlNode>> globalSessions = new ConcurrentHashMap<>();
    
    /**
     * 全局统计信息
     */
    private final SqlTraceStatistics globalStatistics = new SqlTraceStatistics();
    
    /**
     * 慢SQL阈值(毫秒)
     */
    private volatile long slowSqlThreshold;
    
    /**
     * 是否启用追踪
     */
    private volatile boolean traceEnabled;
    
    /**
     * 构造函数，从配置文件初始化
     */
    public SqlCallTreeContext(SqlTreeProperties sqlTreeProperties) {
        this.sqlTreeProperties = sqlTreeProperties;
        this.slowSqlThreshold = sqlTreeProperties.getSlowSqlThreshold();
        this.traceEnabled = sqlTreeProperties.isTraceEnabled();
        log.info("SQL调用树上下文初始化完成，慢SQL阈值: {}ms, 追踪状态: {}", 
                this.slowSqlThreshold, this.traceEnabled ? "启用" : "禁用");
    }
    
    /**
     * 进入SQL调用
     * @param sql SQL语句
     * @param sqlType SQL类型
     * @return SQL节点
     */
    public SqlNode enter(String sql, String sqlType) {
        if (!isTraceEnabled()) {
            return null;
        }
        
        try {
            Stack<SqlNode> sqlStack = callStack.get();
            Stack<ServiceCallInfo> serviceStack = serviceCallStack.get();
            
            // 获取当前Service调用信息
            ServiceCallInfo currentServiceCall = serviceStack.isEmpty() ? null : serviceStack.peek();
            
            // 计算SQL深度：基于Service调用深度
            int sqlDepth;
            if (currentServiceCall != null) {
                // 如果在Service调用中，SQL深度就是Service深度
                // AService深度为1，BService深度为2，以此类推
                sqlDepth = currentServiceCall.getDepth();
                log.info("SQL深度计算: Service={}, ServiceDepth={}, SqlDepth={}", 
                    currentServiceCall.getServiceName(), currentServiceCall.getDepth(), sqlDepth);
            } else {
                // 如果不在Service调用中，使用传统的SQL栈深度
                sqlDepth = sqlStack.size() + 1;
                log.info("SQL深度计算: 无Service调用, SqlStackSize={}, SqlDepth={}", sqlStack.size(), sqlDepth);
            }
            
            SqlNode node = new SqlNode(sql, sqlType, sqlDepth);
            
            // 设置Service调用信息
            if (currentServiceCall != null) {
                node.setServiceName(currentServiceCall.getServiceName());
                node.setMethodName(currentServiceCall.getMethodName());
                node.setServiceCallPath(currentServiceCall.getFullCallPath());
                
                // 将SQL节点添加到Service调用中
                currentServiceCall.addSqlNode(node);
            }
            
            // 建立SQL节点的父子关系
            SqlNode parentSqlNode = findParentSqlNode(sqlStack, serviceStack);
            if (parentSqlNode != null) {
                parentSqlNode.addChild(node);
                log.info("建立父子关系: 父节点[{}] -> 子节点[{}], 父节点children数量={}", 
                    parentSqlNode.getServiceName() + "." + parentSqlNode.getMethodName(), 
                    node.getServiceName() + "." + node.getMethodName(),
                    parentSqlNode.getChildren().size());
            } else {
                // 根节点
                rootNodes.get().add(node);
                log.info("添加根节点: {}", node.getServiceName() + "." + node.getMethodName());
            }
            
            // 将当前节点压入SQL栈
            sqlStack.push(node);
            
            // 更新统计信息
            globalStatistics.incrementTotalSqlCount();
            globalStatistics.updateMaxDepth(sqlDepth);
            
            log.debug("SQL调用进入: depth={}, service={}, sql={}", 
                sqlDepth, 
                currentServiceCall != null ? currentServiceCall.getShortDescription() : "none", 
                sql);
            return node;
            
        } catch (Exception e) {
            log.error("进入SQL调用时发生错误", e);
            return null;
        }
    }
    
    /**
     * 查找SQL节点的父节点
     * 基于Service调用关系确定SQL的父子关系
     */
    private SqlNode findParentSqlNode(Stack<SqlNode> sqlStack, Stack<ServiceCallInfo> serviceStack) {
        // 如果SQL栈不为空，直接返回栈顶节点
        if (!sqlStack.isEmpty()) {
            SqlNode parentSql = sqlStack.peek();
            log.info("查找父节点: SQL栈大小={}, 父节点={}[深度={}]", 
                sqlStack.size(), 
                parentSql.getServiceName() + "." + parentSql.getMethodName(),
                parentSql.getDepth());
            return parentSql;
        }
        
        // 如果SQL栈为空，但有Service调用栈，查找父Service中的最后一个SQL节点
        if (!serviceStack.isEmpty()) {
            ServiceCallInfo currentService = serviceStack.peek();
            if (currentService.getParent() != null) {
                ServiceCallInfo parentService = currentService.getParent();
                List<SqlNode> parentSqlNodes = parentService.getSqlNodes();
                if (!parentSqlNodes.isEmpty()) {
                    SqlNode parentSql = parentSqlNodes.get(parentSqlNodes.size() - 1);
                    log.info("跨Service查找父节点: 父Service={}, 父节点={}[深度={}]", 
                        parentService.getServiceName(),
                        parentSql.getServiceName() + "." + parentSql.getMethodName(),
                        parentSql.getDepth());
                    return parentSql;
                }
            }
        }
        
        log.info("未找到父节点，将作为根节点");
        return null;
    }
    
    /**
     * 退出SQL调用
     * @param node SQL节点
     * @param affectedRows 影响行数
     * @param errorMessage 错误信息
     */
    public void exit(SqlNode node, int affectedRows, String errorMessage) {
        if (!isTraceEnabled() || node == null) {
            return;
        }
        
        try {
            Stack<SqlNode> stack = callStack.get();
            
            if (!stack.isEmpty() && stack.peek().getNodeId().equals(node.getNodeId())) {
                // 弹出栈顶节点
                SqlNode currentNode = stack.pop();
                
                // 设置结束时间和相关信息
                currentNode.setEndTime();
                currentNode.setAffectedRows(affectedRows);
                currentNode.setErrorMessage(errorMessage);
                
                // 标记慢SQL
                currentNode.markSlowSql(slowSqlThreshold);
                
                // 更新统计信息
                if (currentNode.isSlowSql()) {
                    globalStatistics.incrementSlowSqlCount();
                }
                
                if (errorMessage != null && !errorMessage.trim().isEmpty()) {
                    globalStatistics.incrementErrorSqlCount();
                }
                
                globalStatistics.addExecutionTime(currentNode.getExecutionTime());
                
                log.debug("SQL调用退出: depth={}, executionTime={}ms, sql={}", 
                         currentNode.getDepth(), currentNode.getExecutionTime(), currentNode.getSql());
                
                // 不在SQL退出时保存，只在Service退出时保存
                // 这样可以确保Service调用树完全构建后再保存
            }
            
        } catch (Exception e) {
            log.error("退出SQL调用时发生错误", e);
        }
    }
    
    /**
     * 获取当前线程的根节点列表
     * @return 根节点列表
     */
    public List<SqlNode> getRootNodes() {
        return new ArrayList<>(rootNodes.get());
    }
    
    /**
     * 获取当前调用深度
     * @return 调用深度
     */
    public int getCurrentDepth() {
        return callStack.get().size();
    }
    
    /**
     * 检查是否启用追踪
     * @return 是否启用追踪
     */
    public boolean isTraceEnabled() {
        return traceEnabled && traceConfig.get().isEnabled();
    }
    
    /**
     * 设置追踪配置
     * @param config 追踪配置
     */
    public void setTraceConfig(SqlTraceConfig config) {
        if (config != null) {
            traceConfig.set(config);
        }
    }
    
    /**
     * 获取追踪配置
     * @return 追踪配置
     */
    public SqlTraceConfig getTraceConfig() {
        SqlTraceConfig config = new SqlTraceConfig();
        config.setEnabled(this.traceEnabled);
        config.setSlowSqlThreshold(this.slowSqlThreshold);
        config.setMaxDepth(sqlTreeProperties.getMaxDepth());
        config.setRecordParameters(sqlTreeProperties.isRecordParameters());
        config.setMaxSessions(sqlTreeProperties.getMaxSessions());
        return config;
    }
    
    /**
     * 清空当前线程的调用树
     */
    public void clear() {
        try {
            // 清空当前线程的本地存储
            callStack.get().clear();
            rootNodes.get().clear();
            serviceCallStack.get().clear();
            
            // 从全局会话中移除当前线程的数据
            String currentThreadId = Thread.currentThread().getName();
            globalSessions.remove(currentThreadId);
            
            // 如果没有其他会话了，也重置全局统计信息
            if (globalSessions.isEmpty()) {
                globalStatistics.reset();
                log.info("所有会话已清空，重置全局统计信息");
            }
            
            log.info("清空当前线程的SQL调用树: {}", currentThreadId);
        } catch (Exception e) {
            log.error("清空调用树时发生错误", e);
        }
    }
    
    /**
     * 清空所有会话的调用树
     */
    public void clearAll() {
        try {
            globalSessions.clear();
            globalStatistics.reset();
            log.info("清空所有会话的SQL调用树");
        } catch (Exception e) {
            log.error("清空所有调用树时发生错误", e);
        }
    }
    
    /**
     * 获取统计信息
     * @return 统计信息
     */
    public SqlTraceStatistics getStatistics() {
        return globalStatistics.copy();
    }
    
    /**
     * 获取所有会话
     * @return 所有会话
     */
    public Map<String, List<SqlNode>> getAllSessions() {
        return new HashMap<>(globalSessions);
    }
    
    /**
     * 获取所有线程ID列表
     * @return 线程ID列表
     */
    public List<String> getAllThreadIds() {
        return new ArrayList<>(globalSessions.keySet());
    }
    

    
    /**
     * 获取指定会话的调用树
     * @param sessionId 会话ID
     * @return 调用树根节点列表
     */
    public List<SqlNode> getSessionTree(String sessionId) {
        List<SqlNode> nodes = globalSessions.get(sessionId);
        return nodes != null ? new ArrayList<>(nodes) : new ArrayList<>();
    }
    
    /**
     * 手动保存当前线程的调用树到全局会话
     */
    public void saveCurrentSession() {
        saveToGlobalSession();
    }
    
    /**
     * 保存当前线程的调用树到全局会话
     */
    private void saveToGlobalSession() {
        try {
            String threadName = Thread.currentThread().getName();
            List<SqlNode> currentRootNodes = rootNodes.get();
            
            if (!currentRootNodes.isEmpty()) {
                // 调试：检查根节点的children状态
                for (SqlNode rootNode : currentRootNodes) {
                    log.info("保存根节点: {}, children数量={}, maxDepth={}, totalNodeCount={}", 
                        rootNode.getServiceName() + "." + rootNode.getMethodName(),
                        rootNode.getChildren().size(),
                        rootNode.getMaxDepth(),
                        rootNode.getTotalNodeCount());
                }
                
                // 使用纯线程名作为键，覆盖之前的数据（确保保存最完整的数据）
                globalSessions.put(threadName, new ArrayList<>(currentRootNodes));
                log.debug("保存SQL调用树到全局会话: thread={}, nodeCount={}", 
                         threadName, currentRootNodes.size());
            }
        } catch (Exception e) {
            log.error("保存调用树到全局会话时发生错误", e);
        }
    }
    
    /**
     * 设置慢SQL阈值
     * @param threshold 阈值(毫秒)
     */
    public void setSlowSqlThreshold(long threshold) {
        this.slowSqlThreshold = threshold;
        log.info("设置慢SQL阈值: {}ms", threshold);
    }
    
    /**
     * 获取慢SQL阈值
     * @return 阈值(毫秒)
     */
    public long getSlowSqlThreshold() {
        return slowSqlThreshold;
    }
    
    /**
     * 设置是否启用追踪
     * @param enabled 是否启用
     */
    public void setTraceEnabled(boolean enabled) {
        this.traceEnabled = enabled;
        log.info("设置SQL追踪状态: {}", enabled ? "启用" : "禁用");
    }
    
    /**
     * SQL追踪配置
     */
    @Data
    public static class SqlTraceConfig {
        /**
         * 是否启用追踪
         */
        private boolean enabled = true;
        
        /**
         * 最大调用深度
         */
        private int maxDepth = 50;
        
        /**
         * 慢SQL阈值(毫秒)
         */
        private long slowSqlThreshold = 1000L;
        
        /**
         * 是否记录SQL参数
         */
        private boolean recordParameters = true;
        
        /**
         * 最大会话数量
         */
        private int maxSessions = 100;
    }
    
    /**
     * SQL追踪统计信息
     */
    @Data
    public static class SqlTraceStatistics {
        /**
         * 总SQL数量
         */
        private final AtomicLong totalSqlCount = new AtomicLong(0);
        
        /**
         * 慢SQL数量
         */
        private final AtomicLong slowSqlCount = new AtomicLong(0);
        
        /**
         * 错误SQL数量
         */
        private final AtomicLong errorSqlCount = new AtomicLong(0);
        
        /**
         * 总执行时间
         */
        private final AtomicLong totalExecutionTime = new AtomicLong(0);
        
        /**
         * 最大调用深度
         */
        private final AtomicInteger maxDepth = new AtomicInteger(0);
        
        /**
         * 统计开始时间
         */
        private final LocalDateTime startTime = LocalDateTime.now();
        
        public void incrementTotalSqlCount() {
            totalSqlCount.incrementAndGet();
        }
        
        public void incrementSlowSqlCount() {
            slowSqlCount.incrementAndGet();
        }
        
        public void incrementErrorSqlCount() {
            errorSqlCount.incrementAndGet();
        }
        
        public void addExecutionTime(long time) {
            totalExecutionTime.addAndGet(time);
        }
        
        public void updateMaxDepth(int depth) {
            maxDepth.updateAndGet(current -> Math.max(current, depth));
        }
        
        public long getTotalSqlCount() {
            return totalSqlCount.get();
        }
        
        public long getSlowSqlCount() {
            return slowSqlCount.get();
        }
        
        public long getErrorSqlCount() {
            return errorSqlCount.get();
        }
        
        public long getTotalExecutionTime() {
            return totalExecutionTime.get();
        }
        
        public int getMaxDepth() {
            return maxDepth.get();
        }
        
        public double getAverageExecutionTime() {
            long total = getTotalSqlCount();
            return total > 0 ? (double) getTotalExecutionTime() / total : 0.0;
        }
        
        public void reset() {
            totalSqlCount.set(0);
            slowSqlCount.set(0);
            errorSqlCount.set(0);
            totalExecutionTime.set(0);
            maxDepth.set(0);
        }
        
        public SqlTraceStatistics copy() {
            SqlTraceStatistics copy = new SqlTraceStatistics();
            copy.totalSqlCount.set(this.totalSqlCount.get());
            copy.slowSqlCount.set(this.slowSqlCount.get());
            copy.errorSqlCount.set(this.errorSqlCount.get());
            copy.totalExecutionTime.set(this.totalExecutionTime.get());
            copy.maxDepth.set(this.maxDepth.get());
            return copy;
        }
    }
    
    // ==================== Service调用管理方法 ====================
    
    /**
     * 进入Service调用
     * @param serviceName Service类名
     * @param methodName 方法名
     * @return Service调用信息
     */
    public ServiceCallInfo enterService(String serviceName, String methodName) {
        if (!isTraceEnabled()) {
            return null;
        }
        
        try {
            Stack<ServiceCallInfo> stack = serviceCallStack.get();
            int depth = stack.size() + 1;
            
            ServiceCallInfo serviceCall = new ServiceCallInfo(serviceName, methodName, depth);
            
            // 如果栈不为空，将当前Service调用添加为栈顶Service的子调用
            if (!stack.isEmpty()) {
                ServiceCallInfo parent = stack.peek();
                parent.addChild(serviceCall);
            }
            
            // 将当前Service调用压入栈
            stack.push(serviceCall);
            
            log.debug("Service调用进入: {}", serviceCall.getShortDescription());
            return serviceCall;
            
        } catch (Exception e) {
            log.error("进入Service调用时发生错误", e);
            return null;
        }
    }
    
    /**
     * 退出Service调用
     * @param serviceCall Service调用信息
     */
    public void exitService(ServiceCallInfo serviceCall) {
        if (!isTraceEnabled() || serviceCall == null) {
            return;
        }
        
        try {
            Stack<ServiceCallInfo> stack = serviceCallStack.get();
            
            if (!stack.isEmpty() && stack.peek().getCallId().equals(serviceCall.getCallId())) {
                // 弹出栈顶Service调用
                ServiceCallInfo currentCall = stack.pop();
                
                // 设置结束时间
                currentCall.setEndTime();
                
                log.debug("Service调用退出: {}", currentCall.getShortDescription());
                
                // 只在最顶层Service调用（深度为1）退出时保存
                // 这样可以确保每个独立的Service调用只保存一次
                if (currentCall.getDepth() == 1) {
                    saveToGlobalSession();
                    log.info("顶层Service调用结束，保存调用树到全局会话: {}", currentCall.getShortDescription());
                }
            } else {
                log.warn("Service调用栈不匹配: expected={}, actual={}", 
                    serviceCall.getCallId(), 
                    stack.isEmpty() ? "empty" : stack.peek().getCallId());
            }
            
        } catch (Exception e) {
            log.error("退出Service调用时发生错误", e);
        }
    }
    
    /**
     * 获取当前Service调用
     * @return 当前Service调用信息
     */
    public ServiceCallInfo getCurrentServiceCall() {
        Stack<ServiceCallInfo> stack = serviceCallStack.get();
        return stack.isEmpty() ? null : stack.peek();
    }
    
    /**
     * 获取Service调用栈的深度
     * @return Service调用深度
     */
    public int getServiceCallDepth() {
        return serviceCallStack.get().size();
    }
    
    /**
     * 清理Service调用栈
     */
    public void clearServiceCallStack() {
        serviceCallStack.get().clear();
    }
}