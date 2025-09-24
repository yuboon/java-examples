package com.example.dynamicrule.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RuleScript {

    private String ruleName;

    private String script;

    private String description;

    private boolean enabled;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    public RuleScript(String ruleName, String script, String description) {
        this.ruleName = ruleName;
        this.script = script;
        this.description = description;
        this.enabled = true;
        this.createTime = LocalDateTime.now();
        this.updateTime = LocalDateTime.now();
    }
}