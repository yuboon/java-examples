package com.example.hotpatch.example;

/**
 * 示例数学辅助类 - 用于演示静态方法替换
 */
public class MathHelper {
    
    public static int divide(int a, int b) {
        // 原始版本有bug：没有处理除零异常
        return a / b;
    }
    
    public static int multiply(int a, int b) {
        return a * b; // 这个方法没问题，不需要修复
    }
}