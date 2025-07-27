package com.example.core;

import com.example.model.FunctionMetrics;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 函数管理器
 * 负责函数的注册、查找、生命周期管理
 */
@Component
public class FunctionManager {
    
    // 函数注册表
    private final Map<String, FunctionDefinition> functions = new ConcurrentHashMap<>();
    
    // 函数指标
    private final Map<String, FunctionMetrics> metrics = new ConcurrentHashMap<>();
    
    /**
     * 函数定义
     */
    public static class FunctionDefinition {
        private String name;
        private String description;
        private String jarPath;
        private String className;
        private long timeoutMs;
        private Map<String, Object> environment;
        private Date createTime;
        private Date updateTime;
        
        public FunctionDefinition(String name, String jarPath, String className) {
            this.name = name;
            this.jarPath = jarPath;
            this.className = className;
            this.timeoutMs = 30000; // 默认30秒
            this.environment = new HashMap<>();
            this.createTime = new Date();
            this.updateTime = new Date();
        }
        
        // Getter和Setter方法
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public String getJarPath() { return jarPath; }
        public void setJarPath(String jarPath) { this.jarPath = jarPath; }
        
        public String getClassName() { return className; }
        public void setClassName(String className) { this.className = className; }
        
        public long getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(long timeoutMs) { this.timeoutMs = timeoutMs; }

        public Map<String, Object> getEnvironment() { return environment; }
        public void setEnvironment(Map<String, Object> environment) { this.environment = environment; }
        
        public Date getCreateTime() { return createTime; }
        public Date getUpdateTime() { return updateTime; }
        public void setUpdateTime(Date updateTime) { this.updateTime = updateTime; }
    }
    
    /**
     * 注册函数
     */
    public void registerFunction(String name, String jarPath, String className) {
        // 验证jar文件是否存在
        File jarFile = new File(jarPath);
        if (!jarFile.exists()) {
            throw new IllegalArgumentException("JAR file not found: " + jarPath);
        }
        
        FunctionDefinition definition = new FunctionDefinition(name, jarPath, className);
        functions.put(name, definition);
        
        // 初始化指标
        metrics.put(name, new FunctionMetrics(name));
        
        System.out.println("Function registered: " + name + " -> " + className);
    }
    
    /**
     * 注册函数（带配置）
     */
    public void registerFunction(String name, String jarPath, String className, 
                               long timeoutMs, Map<String, Object> environment) {
        registerFunction(name, jarPath, className);
        
        FunctionDefinition definition = functions.get(name);
        definition.setTimeoutMs(timeoutMs);
        if (environment != null) {
            definition.setEnvironment(new HashMap<>(environment));
        }
    }
    
    /**
     * 获取函数定义
     */
    public FunctionDefinition getFunction(String name) {
        return functions.get(name);
    }
    
    /**
     * 检查函数是否存在
     */
    public boolean functionExists(String name) {
        return functions.containsKey(name);
    }
    
    /**
     * 获取所有函数名称
     */
    public Set<String> getAllFunctionNames() {
        return new HashSet<>(functions.keySet());
    }
    
    /**
     * 获取所有函数定义
     */
    public Collection<FunctionDefinition> getAllFunctions() {
        return new ArrayList<>(functions.values());
    }
    
    /**
     * 更新函数
     */
    public void updateFunction(String name, String jarPath, String className) {
        if (!functionExists(name)) {
            throw new IllegalArgumentException("Function not found: " + name);
        }
        
        FunctionDefinition definition = functions.get(name);
        definition.setJarPath(jarPath);
        definition.setClassName(className);
        definition.setUpdateTime(new Date());
        
        System.out.println("Function updated: " + name);
    }
    
    /**
     * 删除函数
     */
    public void removeFunction(String name) {
        if (functions.remove(name) != null) {
            metrics.remove(name);
            System.out.println("Function removed: " + name);
        }
    }
    
    /**
     * 获取函数指标
     */
    public FunctionMetrics getFunctionMetrics(String name) {
        return metrics.get(name);
    }
    
    /**
     * 获取所有函数指标
     */
    public Collection<FunctionMetrics> getAllMetrics() {
        return new ArrayList<>(metrics.values());
    }
    
    /**
     * 清理所有函数
     */
    public void clear() {
        functions.clear();
        metrics.clear();
    }
    
    /**
     * 获取函数数量
     */
    public int getFunctionCount() {
        return functions.size();
    }
}