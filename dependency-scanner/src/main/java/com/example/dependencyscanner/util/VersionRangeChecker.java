package com.example.dependencyscanner.util;

import org.apache.maven.artifact.versioning.ArtifactVersion;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 版本范围检查工具类
 * 用于判断指定版本是否在漏洞版本范围内
 * 
 
 */
public class VersionRangeChecker {
    
    private static final Logger logger = LoggerFactory.getLogger(VersionRangeChecker.class);
    
    // 版本范围表达式的正则模式
    private static final Pattern RANGE_PATTERN = Pattern.compile(
        "^(\\[|\\()?([^,\\[\\]\\(\\)]*)?(?:,([^,\\[\\]\\(\\)]*))?(\\]|\\))?$"
    );
    
    // 简单比较表达式的正则模式 (如 <=2.14.1, >=1.0.0, <3.0.0, >2.0.0)
    private static final Pattern SIMPLE_PATTERN = Pattern.compile(
        "^(<=|>=|<|>|=)?\\s*([0-9]+(?:\\.[0-9]+)*(?:-[A-Za-z0-9]+)*)$"
    );
    
    /**
     * 检查指定版本是否在漏洞版本范围内
     * 
     * @param version 要检查的版本
     * @param vulnerableVersions 漏洞版本范围表达式
     * @return true表示版本存在漏洞，false表示安全
     */
    public static boolean isVulnerable(String version, String vulnerableVersions) {
        if (version == null || vulnerableVersions == null) {
            return false;
        }
        
        try {
            // 处理多个范围用逗号分隔的情况
            String[] ranges = vulnerableVersions.split("\\s*,\\s*");
            for (String range : ranges) {
                if (isVersionInRange(version, range.trim())) {
                    return true;
                }
            }
            return false;
            
        } catch (Exception e) {
            logger.warn("版本比较失败: version={}, vulnerableVersions={}", version, vulnerableVersions, e);
            return false;
        }
    }
    
    /**
     * 检查版本是否在指定范围内
     */
    private static boolean isVersionInRange(String version, String range) {
        // 首先尝试简单比较表达式
        Matcher simpleMatcher = SIMPLE_PATTERN.matcher(range);
        if (simpleMatcher.matches()) {
            return checkSimpleRange(version, simpleMatcher.group(1), simpleMatcher.group(2));
        }
        
        // 尝试Maven风格的范围表达式
        Matcher rangeMatcher = RANGE_PATTERN.matcher(range);
        if (rangeMatcher.matches()) {
            return checkMavenRange(version, rangeMatcher);
        }
        
        // 如果都不匹配，尝试精确匹配
        return version.equals(range);
    }
    
    /**
     * 检查简单比较表达式 (<=, >=, <, >, =)
     */
    private static boolean checkSimpleRange(String version, String operator, String targetVersion) {
        ArtifactVersion currentVersion = new DefaultArtifactVersion(version);
        ArtifactVersion target = new DefaultArtifactVersion(targetVersion);
        
        int comparison = currentVersion.compareTo(target);
        
        if (operator == null || operator.equals("=")) {
            return comparison == 0;
        }
        
        switch (operator) {
            case "<=":
                return comparison <= 0;
            case ">=":
                return comparison >= 0;
            case "<":
                return comparison < 0;
            case ">":
                return comparison > 0;
            default:
                return false;
        }
    }
    
    /**
     * 检查Maven风格的范围表达式
     * 格式: [1.0,2.0) 表示 1.0 <= version < 2.0
     *      (1.0,2.0] 表示 1.0 < version <= 2.0
     *      [1.0,) 表示 version >= 1.0
     *      (,2.0] 表示 version <= 2.0
     */
    private static boolean checkMavenRange(String version, Matcher matcher) {
        String leftBracket = matcher.group(1);
        String lowerBound = matcher.group(2);
        String upperBound = matcher.group(3);
        String rightBracket = matcher.group(4);
        
        ArtifactVersion currentVersion = new DefaultArtifactVersion(version);
        
        // 检查下界
        if (lowerBound != null && !lowerBound.isEmpty()) {
            ArtifactVersion lower = new DefaultArtifactVersion(lowerBound);
            int lowerComparison = currentVersion.compareTo(lower);
            
            if ("[".equals(leftBracket)) {
                // 包含下界
                if (lowerComparison < 0) {
                    return false;
                }
            } else {
                // 不包含下界
                if (lowerComparison <= 0) {
                    return false;
                }
            }
        }
        
        // 检查上界
        if (upperBound != null && !upperBound.isEmpty()) {
            ArtifactVersion upper = new DefaultArtifactVersion(upperBound);
            int upperComparison = currentVersion.compareTo(upper);
            
            if ("]".equals(rightBracket)) {
                // 包含上界
                if (upperComparison > 0) {
                    return false;
                }
            } else {
                // 不包含上界
                if (upperComparison >= 0) {
                    return false;
                }
            }
        }
        
        return true;
    }
    
    /**
     * 比较两个版本号
     * 
     * @param version1 版本1
     * @param version2 版本2
     * @return 负数表示version1 < version2，0表示相等，正数表示version1 > version2
     */
    public static int compareVersions(String version1, String version2) {
        if (version1 == null && version2 == null) {
            return 0;
        }
        if (version1 == null) {
            return -1;
        }
        if (version2 == null) {
            return 1;
        }
        
        try {
            ArtifactVersion v1 = new DefaultArtifactVersion(version1);
            ArtifactVersion v2 = new DefaultArtifactVersion(version2);
            return v1.compareTo(v2);
        } catch (Exception e) {
            logger.warn("版本比较失败: {} vs {}", version1, version2, e);
            return version1.compareTo(version2);
        }
    }
    
    /**
     * 检查版本是否为安全版本
     * 
     * @param version 要检查的版本
     * @param safeVersion 安全版本表达式 (如 "2.15.0+", ">=2.15.0")
     * @return true表示是安全版本
     */
    public static boolean isSafeVersion(String version, String safeVersion) {
        if (version == null || safeVersion == null) {
            return false;
        }
        
        try {
            // 处理 "2.15.0+" 格式
            if (safeVersion.endsWith("+")) {
                String baseVersion = safeVersion.substring(0, safeVersion.length() - 1);
                return compareVersions(version, baseVersion) >= 0;
            }
            
            // 处理比较表达式
            Matcher matcher = SIMPLE_PATTERN.matcher(safeVersion);
            if (matcher.matches()) {
                String operator = matcher.group(1);
                String targetVersion = matcher.group(2);
                
                if (operator == null) {
                    operator = ">="; // 默认为大于等于
                }
                
                return checkSimpleRange(version, operator, targetVersion);
            }
            
            // 精确匹配
            return version.equals(safeVersion);
            
        } catch (Exception e) {
            logger.warn("安全版本检查失败: version={}, safeVersion={}", version, safeVersion, e);
            return false;
        }
    }
}