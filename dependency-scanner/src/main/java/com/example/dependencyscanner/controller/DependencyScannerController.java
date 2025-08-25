package com.example.dependencyscanner.controller;

import com.example.dependencyscanner.model.DependencyInfo;
import com.example.dependencyscanner.model.DependencyRisk;
import com.example.dependencyscanner.service.DependencyCollector;
import com.example.dependencyscanner.service.VulnerabilityMatcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 依赖扫描控制器
 * 提供依赖扫描相关的REST API
 * 
 
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class DependencyScannerController {
    
    private static final Logger logger = LoggerFactory.getLogger(DependencyScannerController.class);
    
    @Autowired
    private DependencyCollector dependencyCollector;
    
    @Autowired
    private VulnerabilityMatcher vulnerabilityMatcher;
    
    /**
     * 扫描依赖并返回风险列表
     * 
     * @return 依赖风险列表
     */
    @GetMapping("/dependencies/scan")
    public ResponseEntity<Map<String, Object>> scanDependencies() {
        logger.info("开始执行依赖扫描...");
        
        long startTime = System.currentTimeMillis();
        
        try {
            // 1. 收集依赖
            List<DependencyInfo> dependencies = dependencyCollector.collect();
            logger.info("收集到 {} 个依赖", dependencies.size());
            
            // 2. 匹配漏洞
            List<DependencyRisk> risks = vulnerabilityMatcher.matchVulnerabilities(dependencies);
            
            // 3. 按风险等级排序
            risks = vulnerabilityMatcher.sortByRiskLevel(risks);
            
            // 4. 获取统计信息
            VulnerabilityMatcher.RiskStatistics statistics = vulnerabilityMatcher.getRiskStatistics(risks);
            
            long endTime = System.currentTimeMillis();
            long duration = endTime - startTime;
            
            logger.info("依赖扫描完成，耗时 {} ms，{}", duration, statistics.toString());
            
            // 构建响应
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "扫描完成");
            response.put("data", risks);
            
            // 修复统计信息：总依赖数应该是所有依赖，不是只有有漏洞的
            Map<String, Object> statisticsMap = new HashMap<>();
            statisticsMap.put("totalDependencies", dependencies.size());  // 所有依赖数量
            statisticsMap.put("vulnerableDependencies", risks.size());     // 有漏洞的依赖数量
            statisticsMap.put("criticalCount", statistics.criticalCount);
            statisticsMap.put("highCount", statistics.highCount);
            statisticsMap.put("mediumCount", statistics.mediumCount);
            statisticsMap.put("lowCount", statistics.lowCount);
            statisticsMap.put("scanDuration", duration);
            
            response.put("statistics", statisticsMap);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("依赖扫描失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "扫描失败: " + e.getMessage());
            errorResponse.put("data", List.of());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取所有依赖列表（不进行漏洞匹配）
     * 
     * @return 依赖列表
     */
    @GetMapping("/dependencies")
    public ResponseEntity<Map<String, Object>> getAllDependencies() {
        logger.info("获取所有依赖列表...");
        
        try {
            List<DependencyInfo> dependencies = dependencyCollector.collect();
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "获取成功");
            response.put("data", dependencies);
            response.put("count", dependencies.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取依赖列表失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取失败: " + e.getMessage());
            errorResponse.put("data", List.of());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 获取扫描统计信息
     * 
     * @return 统计信息
     */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> getStatistics() {
        logger.info("获取扫描统计信息...");
        
        try {
            List<DependencyInfo> dependencies = dependencyCollector.collect();
            List<DependencyRisk> risks = vulnerabilityMatcher.matchVulnerabilities(dependencies);
            VulnerabilityMatcher.RiskStatistics statistics = vulnerabilityMatcher.getRiskStatistics(risks);
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", Map.of(
                "totalDependencies", dependencies.size(),
                "vulnerableDependencies", risks.size(),
                "safePercentage", dependencies.size() > 0 ? 
                    Math.round((double)(dependencies.size() - risks.size()) / dependencies.size() * 100) : 100,
                "riskDistribution", Map.of(
                    "critical", statistics.criticalCount,
                    "high", statistics.highCount,
                    "medium", statistics.mediumCount,
                    "low", statistics.lowCount
                )
            ));
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("获取统计信息失败", e);
            
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("success", false);
            errorResponse.put("message", "获取失败: " + e.getMessage());
            
            return ResponseEntity.status(500).body(errorResponse);
        }
    }
    
    /**
     * 调试接口 - 获取详细的依赖收集信息
     * 
     * @return 调试信息
     */
    @GetMapping("/debug/dependencies")
    public ResponseEntity<Map<String, Object>> debugDependencies() {
        logger.info("开始调试依赖收集...");
        
        Map<String, Object> debugInfo = new HashMap<>();
        
        try {
            // 获取基本环境信息
            debugInfo.put("javaVersion", System.getProperty("java.version"));
            debugInfo.put("javaVendor", System.getProperty("java.vendor"));
            debugInfo.put("userDir", System.getProperty("user.dir"));
            debugInfo.put("userHome", System.getProperty("user.home"));
            
            // 获取ClassLoader信息
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            debugInfo.put("classLoaderType", classLoader.getClass().getName());
            debugInfo.put("classLoaderString", classLoader.toString());
            
            // 获取类路径信息
            String classPath = System.getProperty("java.class.path");
            if (classPath != null) {
                String[] paths = classPath.split(java.io.File.pathSeparator);
                debugInfo.put("classPathEntries", paths.length);
                debugInfo.put("classPathDetails", java.util.Arrays.asList(paths));
                
                // 统计jar文件数量
                long jarCount = java.util.Arrays.stream(paths)
                    .filter(path -> path.endsWith(".jar"))
                    .count();
                debugInfo.put("jarFilesInClassPath", jarCount);
            }
            
            // 尝试收集依赖并获取详细信息
            List<DependencyInfo> dependencies = dependencyCollector.collect();
            debugInfo.put("collectedDependencies", dependencies.size());
            
            // 添加依赖详情
            List<Map<String, Object>> dependencyDetails = new java.util.ArrayList<>();
            for (DependencyInfo dep : dependencies) {
                Map<String, Object> depInfo = new HashMap<>();
                depInfo.put("groupId", dep.getGroupId());
                depInfo.put("artifactId", dep.getArtifactId());
                depInfo.put("version", dep.getVersion());
                depInfo.put("jarPath", dep.getJarPath());
                dependencyDetails.add(depInfo);
            }
            debugInfo.put("dependencyDetails", dependencyDetails);
            
            // 检查Maven相关目录
            java.io.File targetDir = new java.io.File(System.getProperty("user.dir"), "target");
            debugInfo.put("targetDirExists", targetDir.exists());
            if (targetDir.exists()) {
                java.io.File[] targetFiles = targetDir.listFiles();
                if (targetFiles != null) {
                    debugInfo.put("targetDirContents", java.util.Arrays.stream(targetFiles)
                        .map(java.io.File::getName)
                        .collect(java.util.stream.Collectors.toList()));
                }
            }
            
            debugInfo.put("success", true);
            
        } catch (Exception e) {
            logger.error("调试依赖收集失败", e);
            debugInfo.put("success", false);
            debugInfo.put("error", e.getMessage());
            debugInfo.put("stackTrace", java.util.Arrays.toString(e.getStackTrace()));
        }
        
        return ResponseEntity.ok(debugInfo);
    }
    
    /**
     * 健康检查接口
     * 
     * @return 健康状态
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "Dependency Scanner");
        response.put("timestamp", System.currentTimeMillis());
        
        return ResponseEntity.ok(response);
    }
}