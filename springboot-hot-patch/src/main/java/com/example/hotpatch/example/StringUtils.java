package com.example.hotpatch.example;

/**
 * 示例工具类 - 用于演示静态方法替换
 */
public class StringUtils {
    
    public static boolean isEmpty(String str) {
        // 原始版本有bug：没有考虑空白字符
        return str == null || str.length() == 0;
    }
    
    public static String trim(String str) {
        return str == null ? null : str.trim();
    }
}