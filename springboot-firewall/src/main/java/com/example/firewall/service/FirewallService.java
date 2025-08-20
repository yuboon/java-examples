package com.example.firewall.service;

import com.example.firewall.entity.FirewallAccessLog;
import com.example.firewall.entity.FirewallStatistics;
import com.example.firewall.mapper.FirewallAccessLogMapper;
import com.example.firewall.mapper.FirewallStatisticsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 防火墙服务类
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class FirewallService {
    
    @Autowired
    private FirewallAccessLogMapper accessLogMapper;
    
    @Autowired
    private FirewallStatisticsMapper statisticsMapper;
    
    /**
     * 异步记录访问日志
     * 
     * @param accessLog 访问日志
     */
    @Async
    public void logAccessAsync(FirewallAccessLog accessLog) {
        try {
            accessLogMapper.insert(accessLog);
            
            // 更新统计数据
            updateStatistics(accessLog);
            
        } catch (Exception e) {
            log.error("异步记录访问日志失败: {}", accessLog, e);
        }
    }
    
    /**
     * 更新统计数据
     * 
     * @param accessLog 访问日志
     */
    private void updateStatistics(FirewallAccessLog accessLog) {
        try {
            LocalDate today = LocalDate.now();
            String apiPath = accessLog.getApiPath();
            
            // 查询今日统计
            FirewallStatistics stats = statisticsMapper.findByDateAndApiPath(today, apiPath);
            
            if (stats == null) {
                // 创建新的统计记录
                stats = new FirewallStatistics();
                stats.setStatDate(today);
                stats.setApiPath(apiPath);
                stats.setTotalRequests(1L);
                stats.setBlockedRequests(accessLog.isBlocked() ? 1L : 0L);
                stats.setAvgResponseTime(BigDecimal.valueOf(accessLog.getResponseTime()));
                stats.setCreatedTime(LocalDateTime.now());
                stats.setUpdatedTime(LocalDateTime.now());
                
                statisticsMapper.insert(stats);
            } else {
                // 更新现有统计记录
                stats.addRequest(accessLog.isBlocked(), accessLog.getResponseTime().longValue());
                stats.setUpdatedTime(LocalDateTime.now());
                
                statisticsMapper.update(stats);
            }
            
        } catch (Exception e) {
            log.error("更新统计数据失败: {}", accessLog, e);
        }
    }
    
    /**
     * 获取访问日志
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param ipAddress IP地址（可选）
     * @param apiPath API路径（可选）
     * @param limit 限制数量
     * @return 访问日志列表
     */
    public List<FirewallAccessLog> getAccessLogs(LocalDateTime startTime, LocalDateTime endTime, 
                                                 String ipAddress, String apiPath, Integer limit) {
        return accessLogMapper.findByTimeRange(startTime, endTime);
    }
    
    /**
     * 获取统计数据
     * 
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @param apiPath API路径（可选）
     * @return 统计数据列表
     */
    public List<FirewallStatistics> getStatistics(LocalDate startDate, LocalDate endDate, String apiPath) {
        if (apiPath != null && !apiPath.trim().isEmpty()) {
            return statisticsMapper.findByDateRangeAndApiPath(startDate, endDate, apiPath);
        } else {
            return statisticsMapper.findByDateRange(startDate, endDate);
        }
    }
    
    /**
     * 获取今日统计概览
     * 
     * @return 统计概览
     */
    public Map<String, Object> getTodayOverview() {
        Map<String, Object> overview = new ConcurrentHashMap<>();
        LocalDate today = LocalDate.now();
        
        try {
            // 今日总请求数
            Long totalRequests = statisticsMapper.getTotalRequestsByDate(today);
            overview.put("totalRequests", totalRequests != null ? totalRequests : 0L);
            
            // 今日拦截数
            Long blockedRequests = statisticsMapper.getTotalBlockedByDate(today);
            overview.put("blockedRequests", blockedRequests != null ? blockedRequests : 0L);
            
            // 今日平均响应时间
            Double avgResponseTime = statisticsMapper.getAvgResponseTimeByDate(today);
            overview.put("avgResponseTime", avgResponseTime != null ? avgResponseTime : 0.0);
            
            // 拦截率
            if (totalRequests != null && totalRequests > 0) {
                double blockRate = (blockedRequests != null ? blockedRequests : 0L) * 100.0 / totalRequests;
                overview.put("blockRate", Math.round(blockRate * 100.0) / 100.0);
            } else {
                overview.put("blockRate", 0.0);
            }
            
            // 访问量最高的API
            List<Map<String, Object>> topApis = statisticsMapper.getTopApisByRequests(today, 5);
            overview.put("topApis", topApis);
            
            // 拦截率最高的API
            List<Map<String, Object>> topBlockedApis = statisticsMapper.getTopApisByBlockRate(today, 5);
            overview.put("topBlockedApis", topBlockedApis);
            
        } catch (Exception e) {
            log.error("获取今日统计概览失败", e);
        }
        
        return overview;
    }
    
    /**
     * 获取最近N天的趋势数据
     * 
     * @param days 天数
     * @return 趋势数据
     */
    public List<Map<String, Object>> getTrendData(int days) {
        List<FirewallStatistics> trends = statisticsMapper.getDailyTrend(days);
        return trends.stream()
                .map(stat -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", stat.getStatDate());
                    map.put("totalRequests", stat.getTotalRequests());
                    map.put("blockedRequests", stat.getBlockedRequests());
                    map.put("avgResponseTime", stat.getAvgResponseTime());
                    return map;
                })
                .collect(java.util.stream.Collectors.toList());
    }
    
    /**
     * 获取访问最频繁的IP地址
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return IP地址列表
     */
    public List<Map<String, Object>> getTopIpAddresses(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        return accessLogMapper.getTopIpAddresses(startTime, endTime, limit);
    }
    
    /**
     * 获取访问最频繁的API路径
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param limit 限制数量
     * @return API路径列表
     */
    public List<Map<String, Object>> getTopApiPaths(LocalDateTime startTime, LocalDateTime endTime, int limit) {
        return accessLogMapper.getTopApiPaths(startTime, endTime, limit);
    }
    
    /**
     * 统计指定时间范围内的请求数
     * 
     * @param startTime 开始时间
     * @param endTime 结束时间
     * @param ipAddress IP地址（可选）
     * @param apiPath API路径（可选）
     * @return 请求数
     */
    public long countRequests(LocalDateTime startTime, LocalDateTime endTime, String ipAddress, String apiPath) {
        return accessLogMapper.countByTimeRange(startTime, endTime);
    }
    
    /**
     * 定时清理旧的访问日志（每天凌晨2点执行）
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanOldAccessLogs() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(30); // 保留30天
            int deleted = accessLogMapper.deleteOldLogs(cutoffTime);
            log.info("清理30天前的访问日志，删除 {} 条记录", deleted);
        } catch (Exception e) {
            log.error("清理旧访问日志失败", e);
        }
    }
    
    /**
     * 定时清理旧的统计数据（每天凌晨3点执行）
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanOldStatistics() {
        try {
            LocalDate cutoffDate = LocalDate.now().minusDays(90); // 保留90天
            int deleted = statisticsMapper.deleteBeforeDate(cutoffDate);
            log.info("清理90天前的统计数据，删除 {} 条记录", deleted);
        } catch (Exception e) {
            log.error("清理旧统计数据失败", e);
        }
    }
    
    /**
     * 获取系统健康状态
     * 
     * @return 健康状态
     */
    public Map<String, Object> getHealthStatus() {
        Map<String, Object> health = new ConcurrentHashMap<>();
        
        try {
            // 检查数据库连接
            long logCount = accessLogMapper.countAll();
            health.put("database", "UP");
            health.put("totalLogs", logCount);
            
            // 检查今日数据
            Map<String, Object> todayOverview = getTodayOverview();
            health.put("todayStats", todayOverview);
            
            health.put("status", "UP");
            health.put("timestamp", LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("获取系统健康状态失败", e);
            health.put("database", "DOWN");
            health.put("status", "DOWN");
            health.put("error", e.getMessage());
            health.put("timestamp", LocalDateTime.now());
        }
        
        return health;
    }
}