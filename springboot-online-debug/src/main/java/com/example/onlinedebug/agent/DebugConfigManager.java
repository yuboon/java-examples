package com.example.onlinedebug.agent;

import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * 调试配置管理器
 * 管理哪些方法需要被调试
 */
public class DebugConfigManager {
    
    // 精确匹配的方法集合
    private static final Set<String> exactMethods = new CopyOnWriteArraySet<>();
    
    // 模式匹配的方法集合
    private static final Set<Pattern> patternMethods = new CopyOnWriteArraySet<>();
    
    // 类级别的调试开关
    private static final Set<String> debugClasses = new CopyOnWriteArraySet<>();
    
    // 包级别的调试开关
    private static final Set<String> debugPackages = new CopyOnWriteArraySet<>();
    
    // 全局调试开关（谨慎使用，性能影响巨大）
    private static volatile boolean globalDebugEnabled = false;
    
    // 静态初始化块 - 设置默认的调试规则
    static {
        // 清除任何可能存在的旧规则
        exactMethods.clear();
        debugClasses.clear();
        debugPackages.clear();
        
        // 添加默认的演示调试规则
        //exactMethods.add("com.example.onlinedebug.demo.DemoService.getUserById");
        //exactMethods.add("com.example.onlinedebug.demo.DemoController.getUser");
        
        System.out.println("[DEBUG-CONFIG-INIT] Default debug rules initialized: " + exactMethods);
    }
    
    /**
     * 判断是否应该对某个类进行字节码增强
     * （在 Agent 类加载时调用，用于决定是否需要对类进行增强）
     */
    public static boolean shouldDebugClass(String className) {
        // 如果有针对这个类的调试规则，就需要增强
        if (debugClasses.contains(className)) {
            return true;
        }
        
        // 如果有针对这个类所在包的调试规则，就需要增强
        for (String debugPackage : debugPackages) {
            if (className.startsWith(debugPackage)) {
                return true;
            }
        }
        
        // 如果有方法级别的调试规则涉及这个类，就需要增强
        for (String methodName : exactMethods) {
            if (methodName.startsWith(className + ".")) {
                return true;
            }
        }
        
        // 默认不增强未配置的类
        return false;
    }

    /**
     * 判断是否应该调试指定方法
     * 
     * @param fullMethodName 完整方法名，格式为 "类名.方法名"
     * @return true 如果需要调试
     */
    public static boolean shouldDebug(String fullMethodName) {
        // 全局开关（性能考虑，一般不建议开启）
        if (globalDebugEnabled) {
            return true;
        }
        
        // 精确匹配
        if (exactMethods.contains(fullMethodName)) {
            return true;
        }
        
        // 提取类名和包名
        int lastDotIndex = fullMethodName.lastIndexOf('.');
        if (lastDotIndex > 0) {
            String className = fullMethodName.substring(0, lastDotIndex);
            
            // 类级别匹配
            if (debugClasses.contains(className)) {
                return true;
            }
            
            // 包级别匹配
            for (String debugPackage : debugPackages) {
                if (className.startsWith(debugPackage)) {
                    return true;
                }
            }
        }
        
        // 模式匹配
        for (Pattern pattern : patternMethods) {
            if (pattern.matcher(fullMethodName).matches()) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 添加精确的方法调试规则
     * 
     * @param fullMethodName 完整方法名，格式为 "类名.方法名"
     */
    public static void addMethodDebug(String fullMethodName) {
        exactMethods.add(fullMethodName);
    }
    
    /**
     * 移除精确的方法调试规则
     */
    public static void removeMethodDebug(String fullMethodName) {
        exactMethods.remove(fullMethodName);
    }
    
    /**
     * 添加模式匹配的方法调试规则
     * 
     * @param pattern 正则表达式模式
     */
    public static void addPatternDebug(String pattern) {
        patternMethods.add(Pattern.compile(pattern));
    }
    
    /**
     * 移除模式匹配的方法调试规则
     */
    public static void removePatternDebug(String pattern) {
        patternMethods.removeIf(p -> p.pattern().equals(pattern));
    }
    
    /**
     * 添加类级别调试规则
     * 
     * @param className 完整类名
     */
    public static void addClassDebug(String className) {
        debugClasses.add(className);
    }
    
    /**
     * 移除类级别调试规则
     */
    public static void removeClassDebug(String className) {
        debugClasses.remove(className);
    }
    
    /**
     * 添加包级别调试规则
     * 
     * @param packageName 包名
     */
    public static void addPackageDebug(String packageName) {
        debugPackages.add(packageName);
    }
    
    /**
     * 移除包级别调试规则
     */
    public static void removePackageDebug(String packageName) {
        debugPackages.remove(packageName);
    }
    
    /**
     * 设置全局调试开关
     * 警告：开启全局调试会严重影响性能，仅用于特殊情况
     */
    public static void setGlobalDebug(boolean enabled) {
        globalDebugEnabled = enabled;
    }
    
    /**
     * 清除所有调试规则
     */
    public static void clearAllRules() {
        exactMethods.clear();
        patternMethods.clear();
        debugClasses.clear();
        debugPackages.clear();
        globalDebugEnabled = false;
    }
    
    /**
     * 获取当前所有调试规则的状态
     */
    public static DebugConfigStatus getStatus() {
        return new DebugConfigStatus(
            exactMethods.size(),
            patternMethods.size(),
            debugClasses.size(),
            debugPackages.size(),
            globalDebugEnabled
        );
    }
    
    /**
     * 调试配置状态信息
     */
    public static class DebugConfigStatus {
        private final int exactMethodCount;
        private final int patternCount;
        private final int classCount;
        private final int packageCount;
        private final boolean globalEnabled;
        
        public DebugConfigStatus(int exactMethodCount, int patternCount, 
                               int classCount, int packageCount, boolean globalEnabled) {
            this.exactMethodCount = exactMethodCount;
            this.patternCount = patternCount;
            this.classCount = classCount;
            this.packageCount = packageCount;
            this.globalEnabled = globalEnabled;
        }
        
        // Getters
        public int getExactMethodCount() { return exactMethodCount; }
        public int getPatternCount() { return patternCount; }
        public int getClassCount() { return classCount; }
        public int getPackageCount() { return packageCount; }
        public boolean isGlobalEnabled() { return globalEnabled; }
        
        public int getTotalRuleCount() {
            return exactMethodCount + patternCount + classCount + packageCount + (globalEnabled ? 1 : 0);
        }
    }
}