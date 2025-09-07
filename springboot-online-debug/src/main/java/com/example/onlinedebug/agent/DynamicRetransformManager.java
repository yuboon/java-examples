package com.example.onlinedebug.agent;

import java.lang.instrument.Instrumentation;
import java.lang.instrument.UnmodifiableClassException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 动态重转换管理器
 * 负责在运行时重新转换类以应用新的调试规则
 */
public class DynamicRetransformManager {
    
    private static final Set<String> transformedClasses = new HashSet<>();
    
    /**
     * 重新转换指定的类以应用调试规则
     */
    public static void retransformClass(String className) {
        Instrumentation instrumentation = OnlineDebugAgent.getInstrumentation();
        if (instrumentation == null) {
            System.err.println("Warning: Instrumentation not available, cannot retransform class: " + className);
            return;
        }
        
        try {
            // 查找已加载的类
            Class<?> targetClass = findLoadedClass(className);
            if (targetClass != null) {
                // 检查是否可以重新转换
                if (instrumentation.isRetransformClassesSupported() && 
                    instrumentation.isModifiableClass(targetClass)) {
                    
                    System.out.println("[DEBUG-AGENT] Retransforming class: " + className);
                    instrumentation.retransformClasses(targetClass);
                    transformedClasses.add(className);
                    
                } else {
                    System.out.println("[DEBUG-AGENT] Class not modifiable: " + className);
                }
            } else {
                System.out.println("[DEBUG-AGENT] Class not loaded yet: " + className);
            }
        } catch (UnmodifiableClassException e) {
            System.err.println("Failed to retransform class: " + className + ", error: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Error retransforming class: " + className + ", error: " + e.getMessage());
        }
    }
    
    /**
     * 重新转换包下的所有已加载类
     */
    public static void retransformPackage(String packageName) {
        Instrumentation instrumentation = OnlineDebugAgent.getInstrumentation();
        if (instrumentation == null) {
            return;
        }
        
        Class<?>[] allClasses = instrumentation.getAllLoadedClasses();
        for (Class<?> clazz : allClasses) {
            if (clazz.getName().startsWith(packageName)) {
                retransformClass(clazz.getName());
            }
        }
    }
    
    /**
     * 查找已加载的类
     */
    private static Class<?> findLoadedClass(String className) {
        Instrumentation instrumentation = OnlineDebugAgent.getInstrumentation();
        if (instrumentation == null) {
            return null;
        }
        
        Class<?>[] allClasses = instrumentation.getAllLoadedClasses();
        return Arrays.stream(allClasses)
                .filter(clazz -> clazz.getName().equals(className))
                .findFirst()
                .orElse(null);
    }
    
    /**
     * 获取已转换的类集合
     */
    public static Set<String> getTransformedClasses() {
        return new HashSet<>(transformedClasses);
    }
}