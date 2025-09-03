package com.example.hotpatch.model;

import com.example.hotpatch.annotation.HotPatch;
import com.example.hotpatch.annotation.PatchType;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 补丁信息类
 */
@Data
@AllArgsConstructor
public class PatchInfo {
    private String name;
    private String version;
    private Class<?> patchClass;
    private PatchType patchType;
    private long loadTime;
    private String originalTarget; // 原始目标（Bean名称或类名）
    
    public PatchInfo(String name, String version, Class<?> patchClass, 
                    PatchType patchType, long loadTime) {
        this.name = name;
        this.version = version;
        this.patchClass = patchClass;
        this.patchType = patchType;
        this.loadTime = loadTime;
        this.originalTarget = extractOriginalTarget(patchClass);
    }
    
    private String extractOriginalTarget(Class<?> patchClass) {
        HotPatch annotation = patchClass.getAnnotation(HotPatch.class);
        if (annotation != null) {
            switch (annotation.type()) {
                case SPRING_BEAN:
                    return annotation.originalBean();
                case JAVA_CLASS:
                    return annotation.originalClass();
                case STATIC_METHOD:
                case INSTANCE_METHOD:
                    return annotation.originalClass() + "." + annotation.methodName();
                default:
                    return "Unknown";
            }
        }
        return "Unknown";
    }
}