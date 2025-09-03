package com.example.hotpatch.patches;

import com.example.hotpatch.annotation.HotPatch;
import com.example.hotpatch.annotation.PatchType;

/**
 * MathHelper divide方法的补丁类 - 修复除零异常
 */
@HotPatch(
    type = PatchType.STATIC_METHOD,
    originalClass = "com.example.hotpatch.example.MathHelper",
    methodName = "divide",
    version = "1.0.3",
    description = "修复divide方法除零异常"
)
public class MathHelperDividePatch {
    
    public static int divide(int a, int b) {
        // 修复除零异常
        if (b == 0) {
            throw new IllegalArgumentException("除数不能为零");
        }
        return a / b;
    }
}