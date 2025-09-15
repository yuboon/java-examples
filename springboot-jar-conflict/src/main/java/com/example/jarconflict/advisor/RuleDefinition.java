package com.example.jarconflict.advisor;

import java.util.List;

public class RuleDefinition {
    private List<String> patterns;
    private String advice;
    private String severity; // 改为String类型，便于配置绑定
    
    public RuleDefinition() {}

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }
    
    public boolean matches(String input) {
        if (patterns == null || patterns.isEmpty()) {
            return false;
        }
        
        String lowerInput = input.toLowerCase();
        return patterns.stream()
            .anyMatch(pattern -> lowerInput.matches(pattern.toLowerCase()));
    }
}