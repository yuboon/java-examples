package com.example.onlinedebug.service;

import com.example.onlinedebug.agent.DebugConfigManager;
import com.example.onlinedebug.agent.OnlineDebugAgent;
import org.springframework.stereotype.Service;

import java.lang.instrument.Instrumentation;
import java.lang.reflect.Method;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 在线调试服务
 * 提供类和方法信息查询服务
 */
@Service
public class OnlineDebugService {
    
    /**
     * 获取已加载的类信息
     * 返回按包分组的类列表，用于前端展示
     */
    public Map<String, Object> getLoadedClasses() {
        Instrumentation instrumentation = OnlineDebugAgent.getInstrumentation();
        if (instrumentation == null) {
            throw new RuntimeException("Debug agent not initialized");
        }
        
        // 获取所有已加载的类
        Class<?>[] allClasses = instrumentation.getAllLoadedClasses();
        
        // 按包分组，过滤掉JVM内部类
        Map<String, List<String>> packageToClasses = new HashMap<>();
        Set<String> uniqueClasses = new HashSet<>();
        
        for (Class<?> clazz : allClasses) {
            String className = clazz.getName();
            
            // 过滤掉一些不需要调试的类
            if (shouldIncludeClass(className)) {
                uniqueClasses.add(className);
                
                // 按包分组
                String packageName = getPackageName(className);
                packageToClasses.computeIfAbsent(packageName, k -> new ArrayList<>()).add(className);
            }
        }
        
        // 排序
        packageToClasses.forEach((pkg, classes) -> classes.sort(String::compareTo));
        
        Map<String, Object> result = new HashMap<>();
        result.put("totalClasses", uniqueClasses.size());
        result.put("packageCount", packageToClasses.size());
        result.put("packages", packageToClasses);
        
        return result;
    }
    
    /**
     * 获取指定类的方法信息
     */
    public Map<String, Object> getClassMethods(String className) {
        try {
            Class<?> clazz = Class.forName(className);
            Method[] methods = clazz.getDeclaredMethods();
            
            List<Map<String, Object>> methodInfos = new ArrayList<>();
            
            for (Method method : methods) {
                Map<String, Object> methodInfo = new HashMap<>();
                methodInfo.put("name", method.getName());
                methodInfo.put("fullName", className + "." + method.getName());
                methodInfo.put("returnType", method.getReturnType().getSimpleName());
                methodInfo.put("parameterCount", method.getParameterCount());
                
                // 参数类型信息
                Class<?>[] paramTypes = method.getParameterTypes();
                String[] paramTypeNames = new String[paramTypes.length];
                for (int i = 0; i < paramTypes.length; i++) {
                    paramTypeNames[i] = paramTypes[i].getSimpleName();
                }
                methodInfo.put("parameterTypes", paramTypeNames);
                
                // 方法签名
                StringBuilder signature = new StringBuilder();
                signature.append(method.getReturnType().getSimpleName())
                         .append(" ")
                         .append(method.getName())
                         .append("(");
                
                for (int i = 0; i < paramTypeNames.length; i++) {
                    if (i > 0) signature.append(", ");
                    signature.append(paramTypeNames[i]);
                }
                signature.append(")");
                
                methodInfo.put("signature", signature.toString());
                methodInfos.add(methodInfo);
            }
            
            // 按方法名排序
            methodInfos.sort((a, b) -> ((String) a.get("name")).compareTo((String) b.get("name")));
            
            Map<String, Object> result = new HashMap<>();
            result.put("className", className);
            result.put("methodCount", methodInfos.size());
            result.put("methods", methodInfos);
            
            return result;
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("Class not found: " + className, e);
        }
    }
    
    /**
     * 判断是否应该包含某个类
     */
    private boolean shouldIncludeClass(String className) {
        // 排除JVM内部类和一些框架类
        if (className.startsWith("java.") ||
            className.startsWith("javax.") ||
            className.startsWith("sun.") ||
            className.startsWith("com.sun.") ||
            className.startsWith("jdk.") ||
            className.startsWith("net.bytebuddy.") ||
            className.contains("$$EnhancerBy") ||
            className.contains("$$FastClass") ||
            className.contains("CGLIB$$") ||
            className.startsWith("org.springframework.cglib.") ||
            className.startsWith("org.apache.catalina.") ||
            className.startsWith("org.apache.tomcat.")) {
            return false;
        }
        
        // 包含业务类和主要框架类
        return true;
    }
    
    /**
     * 获取包名
     */
    private String getPackageName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot > 0) {
            return className.substring(0, lastDot);
        }
        return "(default)";
    }
    
    /**
     * 搜索类
     */
    public List<String> searchClasses(String keyword) {
        Map<String, Object> allClasses = getLoadedClasses();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> packages = (Map<String, List<String>>) allClasses.get("packages");
        
        return packages.values().stream()
                .flatMap(List::stream)
                .filter(className -> className.toLowerCase().contains(keyword.toLowerCase()))
                .limit(50) // 限制结果数量
                .sorted()
                .collect(Collectors.toList());
    }
    
    /**
     * 搜索方法
     */
    public List<Map<String, Object>> searchMethods(String keyword) {
        Map<String, Object> allClasses = getLoadedClasses();
        @SuppressWarnings("unchecked")
        Map<String, List<String>> packages = (Map<String, List<String>>) allClasses.get("packages");
        
        List<Map<String, Object>> results = new ArrayList<>();
        int count = 0;
        
        for (List<String> classList : packages.values()) {
            for (String className : classList) {
                if (count >= 100) break; // 限制搜索结果
                
                try {
                    Map<String, Object> classInfo = getClassMethods(className);
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> methods = (List<Map<String, Object>>) classInfo.get("methods");
                    
                    for (Map<String, Object> method : methods) {
                        String methodName = (String) method.get("name");
                        String fullName = (String) method.get("fullName");
                        
                        if (methodName.toLowerCase().contains(keyword.toLowerCase()) ||
                            fullName.toLowerCase().contains(keyword.toLowerCase())) {
                            
                            Map<String, Object> result = new HashMap<>(method);
                            result.put("className", className);
                            results.add(result);
                            count++;
                            
                            if (count >= 100) break;
                        }
                    }
                } catch (Exception e) {
                    // 忽略无法加载的类
                }
            }
            if (count >= 100) break;
        }
        
        return results;
    }
    
    /**
     * 获取当前的调试规则
     */
    public Map<String, Object> getDebugRules() {
        Map<String, Object> result = new HashMap<>();
        
        // 获取基本状态
        DebugConfigManager.DebugConfigStatus status = DebugConfigManager.getStatus();
        
        // 获取详细规则信息
        Map<String, Object> rules = new HashMap<>();
        
        // 精确方法规则 - 通过反射获取
        List<String> exactMethods = getExactMethods();
        List<Map<String, Object>> methodRules = new ArrayList<>();
        for (String method : exactMethods) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("type", "method");
            rule.put("target", method);
            rule.put("description", "精确方法调试: " + method);
            methodRules.add(rule);
        }
        
        // 类级别规则 - 通过反射获取
        List<String> debugClasses = getDebugClasses();
        List<Map<String, Object>> classRules = new ArrayList<>();
        for (String className : debugClasses) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("type", "class");
            rule.put("target", className);
            rule.put("description", "类级别调试: " + className);
            classRules.add(rule);
        }
        
        // 包级别规则
        List<String> debugPackages = getDebugPackages();
        List<Map<String, Object>> packageRules = new ArrayList<>();
        for (String packageName : debugPackages) {
            Map<String, Object> rule = new HashMap<>();
            rule.put("type", "package");
            rule.put("target", packageName);
            rule.put("description", "包级别调试: " + packageName);
            packageRules.add(rule);
        }
        
        rules.put("methodRules", methodRules);
        rules.put("classRules", classRules);
        rules.put("packageRules", packageRules);
        
        // 合并所有规则
        List<Map<String, Object>> allRules = new ArrayList<>();
        allRules.addAll(methodRules);
        allRules.addAll(classRules);
        allRules.addAll(packageRules);
        
        result.put("status", Map.of(
            "exactMethodCount", status.getExactMethodCount(),
            "classCount", status.getClassCount(),
            "packageCount", status.getPackageCount(),
            "globalEnabled", status.isGlobalEnabled(),
            "totalRuleCount", status.getTotalRuleCount()
        ));
        result.put("rules", rules);
        result.put("allRules", allRules);
        
        return result;
    }
    
    /**
     * 通过反射获取精确方法列表
     */
    @SuppressWarnings("unchecked")
    private List<String> getExactMethods() {
        try {
            java.lang.reflect.Field field = DebugConfigManager.class.getDeclaredField("exactMethods");
            field.setAccessible(true);
            Set<String> exactMethods = (Set<String>) field.get(null);
            return new ArrayList<>(exactMethods);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 通过反射获取调试类列表
     */
    @SuppressWarnings("unchecked")
    private List<String> getDebugClasses() {
        try {
            java.lang.reflect.Field field = DebugConfigManager.class.getDeclaredField("debugClasses");
            field.setAccessible(true);
            Set<String> debugClasses = (Set<String>) field.get(null);
            return new ArrayList<>(debugClasses);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
    
    /**
     * 通过反射获取调试包列表
     */
    @SuppressWarnings("unchecked")
    private List<String> getDebugPackages() {
        try {
            java.lang.reflect.Field field = DebugConfigManager.class.getDeclaredField("debugPackages");
            field.setAccessible(true);
            Set<String> debugPackages = (Set<String>) field.get(null);
            return new ArrayList<>(debugPackages);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}