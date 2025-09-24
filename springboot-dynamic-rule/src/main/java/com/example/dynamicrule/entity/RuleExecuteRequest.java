package com.example.dynamicrule.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleExecuteRequest {

    private String ruleName;

    private Object params;
}