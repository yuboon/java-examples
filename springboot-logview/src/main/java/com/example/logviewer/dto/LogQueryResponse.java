package com.example.logviewer.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 日志查询响应对象
 * 
 * @author example
 * @version 1.0.0
 */
@Data
public class LogQueryResponse {
    
    /**
     * 日志内容列表
     */
    private List<String> lines;
    
    /**
     * 总行数
     */
    private long totalLines;
    
    /**
     * 当前页码
     */
    private int currentPage;
    
    /**
     * 总页数
     */
    private int totalPages;
    
    /**
     * 文件大小（字节）
     */
    private long fileSize;
    
    /**
     * 最后修改时间
     */
    private LocalDateTime lastModified;
}