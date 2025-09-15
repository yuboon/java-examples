package com.example.jarconflict.advisor;

import java.util.List;

public class SeverityRule {
    private List<String> patterns;
    private String severity; // 改为String类型
    private Integer minJarCount;
    private List<String> conflictTypes; // 改为String类型

    public SeverityRule() {}

    public List<String> getPatterns() {
        return patterns;
    }

    public void setPatterns(List<String> patterns) {
        this.patterns = patterns;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public Integer getMinJarCount() {
        return minJarCount;
    }

    public void setMinJarCount(Integer minJarCount) {
        this.minJarCount = minJarCount;
    }

    public List<String> getConflictTypes() {
        return conflictTypes;
    }

    public void setConflictTypes(List<String> conflictTypes) {
        this.conflictTypes = conflictTypes;
    }
    
    public boolean matches(String identifier, int jarCount, String conflictType) {
        // 检查冲突类型
        if (conflictTypes != null && !conflictTypes.isEmpty() && !conflictTypes.contains(conflictType)) {
            return false;
        }
        
        // 检查Jar数量
        if (minJarCount != null && jarCount < minJarCount) {
            return false;
        }
        
        // 检查模式匹配
        if (patterns != null && !patterns.isEmpty()) {
            String lowerInput = identifier.toLowerCase();
            return patterns.stream()
                .anyMatch(pattern -> lowerInput.matches(pattern.toLowerCase()));
        }
        
        return true;
    }
}