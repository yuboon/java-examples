package com.example.logviewer.util;

import com.example.logviewer.dto.LogQueryRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 日志解析工具类
 * 
 * @author example
 * @version 1.0.0
 */
@Component
public class LogParser {
    
    private static final Pattern LOG_PATTERN = Pattern.compile(
        "(\\d{4}-\\d{2}-\\d{2}\\s+\\d{2}:\\d{2}:\\d{2}\\.\\d{3})\\s+(\\w+)\\s+(.*)");
    
    /**
     * 解析日志行，提取时间和级别
     */
    public LogLineInfo parseLine(String line) {
        Matcher matcher = LOG_PATTERN.matcher(line);
        if (matcher.find()) {
            String timestamp = matcher.group(1);
            String level = matcher.group(2);
            String content = matcher.group(3);
            
            LocalDateTime dateTime = parseTimestamp(timestamp);
            return new LogLineInfo(dateTime, level, content, line);
        }
        
        // 如果不匹配标准格式，返回原始行
        return new LogLineInfo(null, null, line, line);
    }
    
    private LocalDateTime parseTimestamp(String timestamp) {
        try {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
            return LocalDateTime.parse(timestamp, formatter);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 日志行信息类
     */
    @Data
    @AllArgsConstructor
    public static class LogLineInfo {
        private LocalDateTime timestamp;
        private String level;
        private String content;
        private String originalLine;
        
        public boolean matchesFilter(LogQueryRequest request) {
            // 时间范围过滤
            if (timestamp != null) {
                if (request.getStartTime() != null && timestamp.isBefore(request.getStartTime())) {
                    return false;
                }
                if (request.getEndTime() != null && timestamp.isAfter(request.getEndTime())) {
                    return false;
                }
            }
            
            // 日志级别过滤
            if (StringUtils.isNotBlank(request.getLevel()) && 
                !StringUtils.equalsIgnoreCase(level, request.getLevel())) {
                return false;
            }
            
            // 关键字过滤
            if (StringUtils.isNotBlank(request.getKeyword()) && 
                !StringUtils.containsIgnoreCase(originalLine, request.getKeyword())) {
                return false;
            }
            
            return true;
        }
    }
}