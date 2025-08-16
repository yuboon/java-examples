package com.example.logviewer.controller;

import com.example.logviewer.dto.LogQueryRequest;
import com.example.logviewer.dto.LogQueryResponse;
import com.example.logviewer.service.LogService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 日志控制器
 * 
 * @author example
 * @version 1.0.0
 */
@RestController
@RequestMapping("/api/logs")
@Slf4j
public class LogController {
    
    @Autowired
    private LogService logService;
    
    /**
     * 获取日志文件列表
     */
    @GetMapping("/files")
    public ResponseEntity<List<Map<String, Object>>> getLogFiles() {
        try {
            List<Map<String, Object>> files = logService.getLogFiles();
            return ResponseEntity.ok(files);
        } catch (Exception e) {
            log.error("获取日志文件列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 查询日志内容
     */
    @PostMapping("/query")
    public ResponseEntity<LogQueryResponse> queryLogs(@Valid @RequestBody LogQueryRequest request) {
        try {
            LogQueryResponse response = logService.queryLogs(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            log.warn("查询参数错误: {}", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("查询日志失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 下载日志文件
     */
    @GetMapping("/download/{fileName}")
    public void downloadLog(
            @PathVariable String fileName,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            HttpServletResponse response) {
        
        try {
            LogQueryRequest request = new LogQueryRequest();
            request.setFileName(fileName);
            request.setKeyword(keyword);
            request.setLevel(level);
            request.setStartTime(startTime);
            request.setEndTime(endTime);
            
            logService.downloadLog(fileName, request, response);
            
        } catch (IllegalArgumentException e) {
            log.warn("下载参数错误: {}", e.getMessage());
            response.setStatus(HttpStatus.BAD_REQUEST.value());
        } catch (Exception e) {
            log.error("下载日志失败", e);
            response.setStatus(HttpStatus.INTERNAL_SERVER_ERROR.value());
        }
    }
}