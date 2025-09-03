package com.example.hotpatch.patches;

import com.example.hotpatch.annotation.HotPatch;
import com.example.hotpatch.annotation.PatchType;

/**
 * StringUtils的补丁类 - 修复isEmpty方法逻辑
 */
@HotPatch(
    type = PatchType.JAVA_CLASS,
    originalClass = "com.example.hotpatch.example.StringUtils",
    version = "1.0.2",
    description = "修复isEmpty方法逻辑，考虑空白字符"
)
public class StringUtilsPatch {
    
    public static boolean isEmpty(String str) {
        System.err.println("this is StringUtilsPatch isEmpty");
        // 修复：考虑空白字符
        return str == null || str.trim().length() == 0;
    }
    
    public static String trim(String str) {
        System.err.println("this is StringUtilsPatch trim");
        return str == null ? null : str.trim();
    }
}