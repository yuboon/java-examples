package com.example.dynamicrule.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleExecuteResponse {

    private boolean success;

    private Object result;

    private String errorMessage;

    public static RuleExecuteResponse success(Object result) {
        return new RuleExecuteResponse(true, result, null);
    }

    public static RuleExecuteResponse error(String errorMessage) {
        return new RuleExecuteResponse(false, null, errorMessage);
    }
}