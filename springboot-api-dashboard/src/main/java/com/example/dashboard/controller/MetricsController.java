package com.example.dashboard.controller;

import com.example.dashboard.aspect.MethodMetricsAspect;
import com.example.dashboard.model.HierarchicalMethodMetrics;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MetricsController {
    
    private final MethodMetricsAspect metricsAspect;

    @GetMapping
    public Map<String, Object> getMetrics(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) String methodFilter) {
        
        Map<String, Object> result = new HashMap<>();
        Map<String, HierarchicalMethodMetrics> snapshot = metricsAspect.getMetricsSnapshot();
        
        // 应用接口名过滤
        if (StringUtils.hasText(methodFilter)) {
            snapshot = snapshot.entrySet().stream()
                    .filter(entry -> entry.getKey().toLowerCase().contains(methodFilter.toLowerCase()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        
        long currentTime = System.currentTimeMillis();
        
        // 如果没有指定时间范围，使用全量数据
        if (startTime == null || endTime == null) {
            snapshot.forEach((methodName, metrics) -> {
                Map<String, Object> metricData = buildMetricData(metrics);
                result.put(methodName, metricData);
            });
        } else {
            // 使用时间范围查询
            snapshot.forEach((methodName, metrics) -> {
                HierarchicalMethodMetrics.TimeRangeMetrics timeRangeMetrics = 
                        metrics.queryTimeRange(startTime, endTime);
                Map<String, Object> metricData = buildTimeRangeMetricData(timeRangeMetrics);
                result.put(methodName, metricData);
            });
        }
        
        return result;
    }

    private Map<String, Object> buildMetricData(HierarchicalMethodMetrics metrics) {
        Map<String, Object> metricData = new HashMap<>();
        metricData.put("total", metrics.getTotalCount().get());
        metricData.put("success", metrics.getSuccessCount().get());
        metricData.put("fail", metrics.getFailCount().get());
        metricData.put("avgTime", Math.round(metrics.getAvgTime() * 100.0) / 100.0);
        metricData.put("maxTime", metrics.getMaxTime());
        metricData.put("minTime", metrics.getMinTime());
        metricData.put("successRate", Math.round(metrics.getSuccessRate() * 100.0) / 100.0);
        metricData.put("lastAccess", metrics.getLastAccessTime());
        return metricData;
    }

    private Map<String, Object> buildTimeRangeMetricData(HierarchicalMethodMetrics.TimeRangeMetrics timeRangeMetrics) {
        Map<String, Object> metricData = new HashMap<>();
        metricData.put("total", timeRangeMetrics.getTotalCount());
        metricData.put("success", timeRangeMetrics.getSuccessCount());
        metricData.put("fail", timeRangeMetrics.getFailCount());
        metricData.put("avgTime", Math.round(timeRangeMetrics.getAvgTime() * 100.0) / 100.0);
        metricData.put("maxTime", timeRangeMetrics.getMaxTime());
        metricData.put("minTime", timeRangeMetrics.getMinTime());
        metricData.put("successRate", Math.round(timeRangeMetrics.getSuccessRate() * 100.0) / 100.0);
        metricData.put("startTime", timeRangeMetrics.getStartTime());
        metricData.put("endTime", timeRangeMetrics.getEndTime());
        return metricData;
    }

    @GetMapping("/summary")
    public Map<String, Object> getSummary(
            @RequestParam(required = false) Long startTime,
            @RequestParam(required = false) Long endTime,
            @RequestParam(required = false) String methodFilter) {
        
        Map<String, HierarchicalMethodMetrics> snapshot = metricsAspect.getMetricsSnapshot();
        
        // 应用接口名过滤
        if (StringUtils.hasText(methodFilter)) {
            snapshot = snapshot.entrySet().stream()
                    .filter(entry -> entry.getKey().toLowerCase().contains(methodFilter.toLowerCase()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        
        Map<String, Object> summary = new HashMap<>();
        
        if (startTime == null || endTime == null) {
            // 全量数据汇总
            summary.put("totalMethods", snapshot.size());
            summary.put("totalCalls", snapshot.values().stream()
                    .mapToLong(m -> m.getTotalCount().get())
                    .sum());
            summary.put("totalErrors", snapshot.values().stream()
                    .mapToLong(m -> m.getFailCount().get())
                    .sum());
            summary.put("avgResponseTime", snapshot.values().stream()
                    .mapToDouble(HierarchicalMethodMetrics::getAvgTime)
                    .average()
                    .orElse(0.0));
        } else {
            // 时间段数据汇总
            long totalCalls = 0;
            long totalErrors = 0;
            double totalAvgTime = 0;
            int methodCount = 0;
            
            for (HierarchicalMethodMetrics metrics : snapshot.values()) {
                HierarchicalMethodMetrics.TimeRangeMetrics timeRange = 
                        metrics.queryTimeRange(startTime, endTime);
                if (timeRange.getTotalCount() > 0) {
                    totalCalls += timeRange.getTotalCount();
                    totalErrors += timeRange.getFailCount();
                    totalAvgTime += timeRange.getAvgTime();
                    methodCount++;
                }
            }
            
            summary.put("totalMethods", methodCount);
            summary.put("totalCalls", totalCalls);
            summary.put("totalErrors", totalErrors);
            summary.put("avgResponseTime", methodCount > 0 ? totalAvgTime / methodCount : 0.0);
            summary.put("timeRange", Map.of("startTime", startTime, "endTime", endTime));
        }
        
        return summary;
    }

    @GetMapping("/recent/{minutes}")
    public Map<String, Object> getRecentMetrics(
            @PathVariable int minutes,
            @RequestParam(required = false) String methodFilter) {
        
        long endTime = System.currentTimeMillis();
        long startTime = endTime - (minutes * 60L * 1000L);
        
        return getMetrics(startTime, endTime, methodFilter);
    }

    @DeleteMapping
    public Map<String, String> clearMetrics() {
        metricsAspect.clearMetrics();
        Map<String, String> response = new HashMap<>();
        response.put("message", "All metrics cleared successfully");
        return response;
    }

    @DeleteMapping("/stale")
    public Map<String, String> removeStaleMetrics(@RequestParam(defaultValue = "3600000") long maxAgeMs) {
        metricsAspect.removeStaleMetrics(maxAgeMs);
        Map<String, String> response = new HashMap<>();
        response.put("message", "Stale metrics removed successfully");
        return response;
    }
}