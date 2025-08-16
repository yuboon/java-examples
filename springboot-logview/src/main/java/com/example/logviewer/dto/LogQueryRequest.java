package com.example.logviewer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;


import java.time.LocalDateTime;

/**
 * 日志查询请求对象
 * 
 * @author example
 * @version 1.0.0
 */
@Data
public class LogQueryRequest {
    
    @NotBlank(message = "文件名不能为空")
    private String fileName;
    
    /**
     * 页码，从1开始
     */
    @Min(value = 1, message = "页码必须大于0")
    private int page = 1;
    
    /**
     * 每页行数
     */
    @Min(value = 1, message = "每页行数必须大于0")
    @Max(value = 1000, message = "每页行数不能超过1000")
    private int pageSize = 100;
    
    /**
     * 关键字搜索
     */
    private String keyword;
    
    /**
     * 日志级别过滤
     */
    private String level;
    
    /**
     * 开始时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime startTime;
    
    /**
     * 结束时间
     */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endTime;
    
    /**
     * 是否倒序
     */
    private boolean reverse = true;
}