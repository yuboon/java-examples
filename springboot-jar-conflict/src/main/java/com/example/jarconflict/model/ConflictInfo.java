package com.example.jarconflict.model;

import java.util.List;

public class ConflictInfo {
    private String className;
    private List<JarInfo> conflictingJars;
    private ConflictType type;
    private String advice;
    private SeverityLevel severity;

    public ConflictInfo() {}

    public ConflictInfo(String className, List<JarInfo> conflictingJars, ConflictType type) {
        this.className = className;
        this.conflictingJars = conflictingJars;
        this.type = type;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    public List<JarInfo> getConflictingJars() {
        return conflictingJars;
    }

    public void setConflictingJars(List<JarInfo> conflictingJars) {
        this.conflictingJars = conflictingJars;
    }

    public ConflictType getType() {
        return type;
    }

    public void setType(ConflictType type) {
        this.type = type;
    }

    public String getAdvice() {
        return advice;
    }

    public void setAdvice(String advice) {
        this.advice = advice;
    }

    public SeverityLevel getSeverity() {
        return severity;
    }

    public void setSeverity(SeverityLevel severity) {
        this.severity = severity;
    }

    public enum ConflictType {
        CLASS_DUPLICATE,
        VERSION_CONFLICT,
        JAR_DUPLICATE
    }

    public enum SeverityLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }
}