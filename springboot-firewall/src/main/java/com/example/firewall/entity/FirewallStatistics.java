package com.example.firewall.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 防火墙统计实体类
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirewallStatistics {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 统计日期
     */
    private LocalDate statDate;
    
    /**
     * API路径
     */
    private String apiPath;
    
    /**
     * 总请求数
     */
    private Long totalRequests;
    
    /**
     * 被拦截请求数
     */
    private Long blockedRequests;
    
    /**
     * 平均响应时间
     */
    private BigDecimal avgResponseTime;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 计算拦截率
     * 
     * @return 拦截率（百分比）
     */
    public double getBlockRate() {
        if (totalRequests == null || totalRequests == 0) {
            return 0.0;
        }
        
        long blocked = blockedRequests != null ? blockedRequests : 0;
        return (double) blocked / totalRequests * 100;
    }
    
    /**
     * 计算成功率
     * 
     * @return 成功率（百分比）
     */
    public double getSuccessRate() {
        return 100.0 - getBlockRate();
    }
    
    /**
     * 获取拦截率描述
     * 
     * @return 拦截率描述
     */
    public String getBlockRateDescription() {
        double rate = getBlockRate();
        
        if (rate == 0) {
            return "无拦截";
        } else if (rate < 1) {
            return "极低 (" + String.format("%.2f", rate) + "%)";
        } else if (rate < 5) {
            return "较低 (" + String.format("%.2f", rate) + "%)";
        } else if (rate < 20) {
            return "中等 (" + String.format("%.2f", rate) + "%)";
        } else {
            return "较高 (" + String.format("%.2f", rate) + "%)";
        }
    }
    
    /**
     * 获取响应时间描述
     * 
     * @return 响应时间描述
     */
    public String getResponseTimeDescription() {
        if (avgResponseTime == null) {
            return "未知";
        }
        
        double time = avgResponseTime.doubleValue();
        
        if (time < 100) {
            return "快速 (" + String.format("%.1f", time) + "ms)";
        } else if (time < 500) {
            return "正常 (" + String.format("%.1f", time) + "ms)";
        } else if (time < 1000) {
            return "较慢 (" + String.format("%.1f", time) + "ms)";
        } else {
            return "缓慢 (" + String.format("%.1f", time) + "ms)";
        }
    }
    
    /**
     * 增加请求统计
     * 
     * @param isBlocked 是否被拦截
     * @param responseTime 响应时间
     */
    public void addRequest(boolean isBlocked, long responseTime) {
        // 增加总请求数
        this.totalRequests = (this.totalRequests != null ? this.totalRequests : 0) + 1;
        
        // 增加拦截数
        if (isBlocked) {
            this.blockedRequests = (this.blockedRequests != null ? this.blockedRequests : 0) + 1;
        }
        
        // 更新平均响应时间
        if (this.avgResponseTime == null) {
            this.avgResponseTime = BigDecimal.valueOf(responseTime);
        } else {
            // 计算新的平均值
            BigDecimal currentTotal = this.avgResponseTime.multiply(BigDecimal.valueOf(this.totalRequests - 1));
            BigDecimal newTotal = currentTotal.add(BigDecimal.valueOf(responseTime));
            this.avgResponseTime = newTotal.divide(BigDecimal.valueOf(this.totalRequests), 2, BigDecimal.ROUND_HALF_UP);
        }
        
        // 更新时间
        this.updatedTime = LocalDateTime.now();
    }
}