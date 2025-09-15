package com.example.jarconflict.advisor;

import com.example.jarconflict.model.ConflictInfo;
import com.example.jarconflict.model.JarInfo;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@ConfigurationProperties(prefix = "conflict.advisor")
public class ConflictAdvisor {
    
    private Map<String, RuleDefinition> rules = new HashMap<>();
    private List<SeverityRule> severityRules = new ArrayList<>();
    private String defaultAdvice = "检测到依赖冲突。建议：\n" +
                                  "1. 使用 mvn dependency:tree 分析依赖关系\n" +
                                  "2. 使用 <exclusion> 排除冲突依赖\n" +
                                  "3. 在dependencyManagement中统一版本管理\n" +
                                  "4. 优先使用Spring Boot的版本管理";

    public void generateAdvice(List<ConflictInfo> conflicts) {
        System.out.println("=== ConflictAdvisor Debug Info ===");
        System.out.println("Loaded rules: " + rules.size());
        System.out.println("Loaded severity rules: " + severityRules.size());
        
        for (ConflictInfo conflict : conflicts) {
            String advice = generateAdviceForConflict(conflict);
            conflict.setAdvice(advice);
            
            // 评估严重程度
            if (conflict.getSeverity() == null) {
                conflict.setSeverity(evaluateSeverity(conflict));
            }
        }
    }

    private String generateAdviceForConflict(ConflictInfo conflict) {
        String identifier = extractIdentifier(conflict);
        System.out.println("Processing conflict: " + identifier);
        
        // 查找匹配的规则
        for (Map.Entry<String, RuleDefinition> entry : rules.entrySet()) {
            RuleDefinition rule = entry.getValue();
            if (rule.matches(identifier)) {
                System.out.println("Matched rule: " + entry.getKey());
                // 设置匹配到的严重程度
                if (rule.getSeverity() != null) {
                    conflict.setSeverity(parseseverity(rule.getSeverity()));
                }
                return formatAdvice(rule.getAdvice(), conflict);
            }
        }
        
        return formatAdvice(defaultAdvice, conflict);
    }

    private String extractIdentifier(ConflictInfo conflict) {
        return switch (conflict.getType()) {
            case CLASS_DUPLICATE -> conflict.getClassName();
            case VERSION_CONFLICT, JAR_DUPLICATE -> conflict.getClassName();
        };
    }

    private String formatAdvice(String template, ConflictInfo conflict) {
        Map<String, String> variables = buildVariables(conflict);
        
        String result = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            result = result.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        
        return result;
    }

    private Map<String, String> buildVariables(ConflictInfo conflict) {
        Map<String, String> variables = new HashMap<>();
        variables.put("className", conflict.getClassName());
        variables.put("conflictType", getConflictTypeText(conflict.getType()));
        variables.put("jarCount", String.valueOf(conflict.getConflictingJars().size()));
        
        List<String> jarNames = conflict.getConflictingJars().stream()
            .map(jar -> jar.getName() + ":" + jar.getVersion())
            .toList();
        variables.put("jars", String.join(", ", jarNames));
        variables.put("jarList", String.join("\n", jarNames));
        
        List<String> versions = conflict.getConflictingJars().stream()
            .map(JarInfo::getVersion)
            .filter(v -> v != null && !v.equals("unknown"))
            .distinct()
            .toList();
        variables.put("versions", String.join(", ", versions));
        
        List<String> paths = conflict.getConflictingJars().stream()
            .map(JarInfo::getPath)
            .toList();
        variables.put("paths", String.join("\n", paths));
        
        return variables;
    }

    private ConflictInfo.SeverityLevel evaluateSeverity(ConflictInfo conflict) {
        String identifier = extractIdentifier(conflict).toLowerCase();
        int jarCount = conflict.getConflictingJars().size();
        String conflictType = conflict.getType().name();
        
        // 按优先级匹配严重程度规则
        for (SeverityRule rule : severityRules) {
            if (rule.matches(identifier, jarCount, conflictType)) {
                return parseseverity(rule.getSeverity());
            }
        }
        
        // 默认严重程度逻辑
        if (jarCount > 3) {
            return ConflictInfo.SeverityLevel.MEDIUM;
        }
        
        return ConflictInfo.SeverityLevel.LOW;
    }
    
    private ConflictInfo.SeverityLevel parseseverity(String severity) {
        if (severity == null) return ConflictInfo.SeverityLevel.LOW;
        
        try {
            return ConflictInfo.SeverityLevel.valueOf(severity.toUpperCase());
        } catch (IllegalArgumentException e) {
            return ConflictInfo.SeverityLevel.LOW;
        }
    }

    private String getConflictTypeText(ConflictInfo.ConflictType type) {
        return switch (type) {
            case CLASS_DUPLICATE -> "类重复";
            case VERSION_CONFLICT -> "版本冲突";
            case JAR_DUPLICATE -> "Jar重复";
        };
    }

    // Configuration Properties Getters and Setters
    public Map<String, RuleDefinition> getRules() { 
        return rules; 
    }
    
    public void setRules(Map<String, RuleDefinition> rules) { 
        this.rules = rules; 
        System.out.println("Rules set: " + rules.keySet());
    }
    
    public List<SeverityRule> getSeverityRules() { 
        return severityRules; 
    }
    
    public void setSeverityRules(List<SeverityRule> severityRules) { 
        this.severityRules = severityRules; 
        System.out.println("Severity rules set: " + severityRules.size());
    }
    
    public String getDefaultAdvice() { 
        return defaultAdvice; 
    }
    
    public void setDefaultAdvice(String defaultAdvice) { 
        this.defaultAdvice = defaultAdvice; 
    }
}