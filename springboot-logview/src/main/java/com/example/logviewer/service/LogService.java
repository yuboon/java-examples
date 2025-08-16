package com.example.logviewer.service;

import com.example.logviewer.config.LogConfig;
import com.example.logviewer.dto.LogQueryRequest;
import com.example.logviewer.dto.LogQueryResponse;
import com.example.logviewer.util.LogParser;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;


import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 日志服务类
 * 
 * @author example
 * @version 1.0.0
 */
@Service
@Slf4j
public class LogService {
    
    @Autowired
    private LogConfig logConfig;
    
    @Autowired
    private LogParser logParser;
    
    /**
     * 获取日志文件列表
     */
    public List<Map<String, Object>> getLogFiles() {
        File logDir = new File(logConfig.getLogPath());
        if (!logDir.exists() || !logDir.isDirectory()) {
            return Collections.emptyList();
        }
        
        return Arrays.stream(logDir.listFiles())
            .filter(this::isValidLogFile)
            .map(this::fileToMap)
            .sorted((a, b) -> ((Long)b.get("lastModified")).compareTo((Long)a.get("lastModified")))
            .collect(Collectors.toList());
    }
    
    /**
     * 查询日志内容
     */
    public LogQueryResponse queryLogs(LogQueryRequest request) {
        File logFile = getLogFile(request.getFileName());
        validateFile(logFile);
        
        try {
            List<String> allLines = FileUtils.readLines(logFile, StandardCharsets.UTF_8);
            
            // 过滤日志行
            List<String> filteredLines = filterLines(allLines, request);
            
            // 倒序处理
            if (request.isReverse()) {
                Collections.reverse(filteredLines);
            }
            
            // 分页处理
            int totalLines = filteredLines.size();
            int totalPages = (int) Math.ceil((double) totalLines / request.getPageSize());
            int startIndex = (request.getPage() - 1) * request.getPageSize();
            int endIndex = Math.min(startIndex + request.getPageSize(), totalLines);
            
            List<String> pageLines = filteredLines.subList(startIndex, endIndex);
            
            LogQueryResponse response = new LogQueryResponse();
            response.setLines(pageLines);
            response.setTotalLines(totalLines);
            response.setCurrentPage(request.getPage());
            response.setTotalPages(totalPages);
            response.setFileSize(logFile.length());
            response.setLastModified(
                LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(logFile.lastModified()), 
                    ZoneId.systemDefault()
                )
            );
            
            return response;
            
        } catch (IOException e) {
            log.error("读取日志文件失败: {}", logFile.getAbsolutePath(), e);
            throw new RuntimeException("读取日志文件失败", e);
        }
    }
    
    /**
     * 下载日志文件
     */
    public void downloadLog(String fileName, LogQueryRequest request, HttpServletResponse response) {
        File logFile = getLogFile(fileName);
        validateFile(logFile);
        
        try {
            response.setContentType("application/octet-stream");
            response.setHeader("Content-Disposition", 
                "attachment; filename=" + URLEncoder.encode(fileName, "UTF-8"));
            
            if (hasFilter(request)) {
                // 下载过滤后的内容
                List<String> allLines = FileUtils.readLines(logFile, StandardCharsets.UTF_8);
                List<String> filteredLines = filterLines(allLines, request);
                
                try (PrintWriter writer = response.getWriter()) {
                    for (String line : filteredLines) {
                        writer.println(line);
                    }
                }
            } else {
                // 下载原文件
                response.setContentLengthLong(logFile.length());
                try (InputStream inputStream = new FileInputStream(logFile);
                     OutputStream outputStream = response.getOutputStream()) {
                    IOUtils.copy(inputStream, outputStream);
                }
            }
            
        } catch (IOException e) {
            log.error("下载日志文件失败: {}", logFile.getAbsolutePath(), e);
            throw new RuntimeException("下载日志文件失败", e);
        }
    }
    
    private List<String> filterLines(List<String> lines, LogQueryRequest request) {
        if (!hasFilter(request)) {
            return lines;
        }
        
        return lines.stream()
            .map(logParser::parseLine)
            .filter(lineInfo -> lineInfo.matchesFilter(request))
            .map(LogParser.LogLineInfo::getOriginalLine)
            .collect(Collectors.toList());
    }
    
    private boolean hasFilter(LogQueryRequest request) {
        return StringUtils.isNotBlank(request.getKeyword()) ||
               StringUtils.isNotBlank(request.getLevel()) ||
               request.getStartTime() != null ||
               request.getEndTime() != null;
    }
    
    private File getLogFile(String fileName) {
        // 安全检查：防止路径遍历攻击
        if (logConfig.isEnableSecurity()) {
            if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                throw new IllegalArgumentException("非法的文件名");
            }
        }
        
        return new File(logConfig.getLogPath(), fileName);
    }
    
    private void validateFile(File file) {
        if (!file.exists()) {
            throw new IllegalArgumentException("文件不存在");
        }
        
        if (!file.isFile()) {
            throw new IllegalArgumentException("不是有效的文件");
        }
        
        if (!isValidLogFile(file)) {
            throw new IllegalArgumentException("不支持的文件类型");
        }
        
        long fileSizeMB = file.length() / (1024 * 1024);
        if (fileSizeMB > logConfig.getMaxFileSize()) {
            throw new IllegalArgumentException(
                String.format("文件过大，超过限制 %dMB", logConfig.getMaxFileSize())
            );
        }
    }
    
    private boolean isValidLogFile(File file) {
        String fileName = file.getName().toLowerCase();
        return logConfig.getAllowedExtensions().stream()
            .anyMatch(fileName::endsWith);
    }
    
    private Map<String, Object> fileToMap(File file) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", file.getName());
        map.put("size", file.length());
        map.put("lastModified", file.lastModified());
        map.put("readable", file.canRead());
        return map;
    }
}