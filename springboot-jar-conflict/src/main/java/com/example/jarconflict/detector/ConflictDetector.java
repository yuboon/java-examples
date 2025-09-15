package com.example.jarconflict.detector;

import com.example.jarconflict.model.ConflictInfo;
import com.example.jarconflict.model.JarInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

@Component
public class ConflictDetector {
    private static final Logger logger = LoggerFactory.getLogger(ConflictDetector.class);

    public List<ConflictInfo> detectConflicts(List<JarInfo> jars) {
        logger.info("Starting conflict detection for {} jars", jars.size());
        
        List<ConflictInfo> conflicts = new ArrayList<>();
        
        conflicts.addAll(detectClassDuplicates(jars));
        conflicts.addAll(detectVersionConflicts(jars));
        conflicts.addAll(detectJarDuplicates(jars));
        
        logger.info("Found {} total conflicts", conflicts.size());
        return conflicts;
    }

    private List<ConflictInfo> detectClassDuplicates(List<JarInfo> jars) {
        logger.debug("Detecting class duplicates...");
        Map<String, List<JarInfo>> classToJarsMap = new HashMap<>();
        
        for (JarInfo jar : jars) {
            if (jar.getClasses() != null) {
                for (String className : jar.getClasses()) {
                    classToJarsMap.computeIfAbsent(className, k -> new ArrayList<>()).add(jar);
                }
            }
        }
        
        return classToJarsMap.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(entry -> {
                ConflictInfo conflict = new ConflictInfo(
                    entry.getKey(), 
                    entry.getValue(), 
                    ConflictInfo.ConflictType.CLASS_DUPLICATE
                );
                conflict.setSeverity(calculateClassConflictSeverity(entry.getKey(), entry.getValue()));
                return conflict;
            })
            .collect(Collectors.toList());
    }

    private List<ConflictInfo> detectVersionConflicts(List<JarInfo> jars) {
        logger.debug("Detecting version conflicts...");
        Map<String, List<JarInfo>> nameToJarsMap = jars.stream()
            .collect(Collectors.groupingBy(JarInfo::getName));
        
        return nameToJarsMap.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .filter(entry -> hasVersionConflict(entry.getValue()))
            .map(entry -> {
                ConflictInfo conflict = new ConflictInfo(
                    entry.getKey(), 
                    entry.getValue(), 
                    ConflictInfo.ConflictType.VERSION_CONFLICT
                );
                conflict.setSeverity(calculateVersionConflictSeverity(entry.getKey(), entry.getValue()));
                return conflict;
            })
            .collect(Collectors.toList());
    }

    private List<ConflictInfo> detectJarDuplicates(List<JarInfo> jars) {
        logger.debug("Detecting jar duplicates...");
        Map<String, List<JarInfo>> pathSignatureMap = new HashMap<>();
        
        for (JarInfo jar : jars) {
            String signature = generateJarSignature(jar);
            pathSignatureMap.computeIfAbsent(signature, k -> new ArrayList<>()).add(jar);
        }
        
        return pathSignatureMap.entrySet().stream()
            .filter(entry -> entry.getValue().size() > 1)
            .map(entry -> {
                String className = "Duplicate JAR: " + entry.getValue().get(0).getName();
                ConflictInfo conflict = new ConflictInfo(
                    className,
                    entry.getValue(), 
                    ConflictInfo.ConflictType.JAR_DUPLICATE
                );
                conflict.setSeverity(ConflictInfo.SeverityLevel.MEDIUM);
                return conflict;
            })
            .collect(Collectors.toList());
    }

    private boolean hasVersionConflict(List<JarInfo> jars) {
        Set<String> versions = jars.stream()
            .map(JarInfo::getVersion)
            .filter(v -> v != null && !v.equals("unknown"))
            .collect(Collectors.toSet());
        return versions.size() > 1;
    }

    private ConflictInfo.SeverityLevel calculateClassConflictSeverity(String className, List<JarInfo> jars) {
        if (isCriticalClass(className)) {
            return ConflictInfo.SeverityLevel.CRITICAL;
        }
        
        if (isSystemClass(className)) {
            return ConflictInfo.SeverityLevel.HIGH;
        }
        
        if (isFrameworkClass(className)) {
            return ConflictInfo.SeverityLevel.HIGH;
        }
        
        if (jars.size() > 3) {
            return ConflictInfo.SeverityLevel.MEDIUM;
        }
        
        return ConflictInfo.SeverityLevel.LOW;
    }

    private ConflictInfo.SeverityLevel calculateVersionConflictSeverity(String jarName, List<JarInfo> jars) {
        if (isCriticalLibrary(jarName)) {
            return ConflictInfo.SeverityLevel.CRITICAL;
        }
        
        if (isFrameworkLibrary(jarName)) {
            return ConflictInfo.SeverityLevel.HIGH;
        }
        
        Set<String> majorVersions = jars.stream()
            .map(jar -> extractMajorVersion(jar.getVersion()))
            .collect(Collectors.toSet());
        
        if (majorVersions.size() > 1) {
            return ConflictInfo.SeverityLevel.HIGH;
        }
        
        return ConflictInfo.SeverityLevel.MEDIUM;
    }

    private boolean isCriticalClass(String className) {
        return className.startsWith("java.") ||
               className.startsWith("javax.") ||
               className.startsWith("org.slf4j.") ||
               className.startsWith("org.apache.logging.") ||
               className.contains("Logger") ||
               className.contains("Driver");
    }

    private boolean isSystemClass(String className) {
        return className.startsWith("org.springframework.") ||
               className.startsWith("org.hibernate.") ||
               className.startsWith("com.fasterxml.jackson.");
    }

    private boolean isFrameworkClass(String className) {
        return className.startsWith("org.apache.") ||
               className.startsWith("com.mysql.") ||
               className.startsWith("org.postgresql.") ||
               className.startsWith("redis.clients.");
    }

    private boolean isCriticalLibrary(String jarName) {
        return jarName.contains("slf4j") ||
               jarName.contains("logback") ||
               jarName.contains("log4j") ||
               jarName.contains("mysql") ||
               jarName.contains("postgresql") ||
               jarName.contains("driver");
    }

    private boolean isFrameworkLibrary(String jarName) {
        return jarName.startsWith("spring") ||
               jarName.contains("hibernate") ||
               jarName.contains("jackson") ||
               jarName.contains("apache");
    }

    private String extractMajorVersion(String version) {
        if (version == null || version.equals("unknown")) {
            return "unknown";
        }
        
        String[] parts = version.split("\\.");
        return parts.length > 0 ? parts[0] : version;
    }

    private String generateJarSignature(JarInfo jar) {
        if (jar.getClasses() == null || jar.getClasses().isEmpty()) {
            return jar.getName() + ":" + jar.getVersion();
        }
        
        return jar.getName() + ":" + jar.getClasses().size();
    }
}