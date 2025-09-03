package com.example.hotpatch.model;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 补丁操作结果类
 */
@Data
@AllArgsConstructor
public class PatchResult {
    private boolean success;
    private String message;
    private Object data;
    
    public static PatchResult success(String message) {
        return new PatchResult(true, message, null);
    }
    
    public static PatchResult success(String message, Object data) {
        return new PatchResult(true, message, data);
    }
    
    public static PatchResult failed(String message) {
        return new PatchResult(false, message, null);
    }
}