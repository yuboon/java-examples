package com.example.dashboard.service;

import com.example.dashboard.aspect.MethodMetricsAspect;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MetricsCleanupService {

    private final MethodMetricsAspect metricsAspect;

    @Value("${dashboard.metrics.max-age:3600000}")
    private long maxAge;

    @Scheduled(fixedRateString = "${dashboard.metrics.cleanup-interval:300000}")
    public void cleanupStaleMetrics() {
        try {
            log.debug("Starting metrics cleanup task...");
            metricsAspect.removeStaleMetrics(maxAge);
            
            int currentMethodCount = metricsAspect.getMetricsSnapshot().size();
            log.info("Metrics cleanup completed. Current methods being monitored: {}", currentMethodCount);
            
        } catch (Exception e) {
            log.error("Error during metrics cleanup", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?") // 每天凌晨2点清理
    public void dailyCleanup() {
        try {
            log.info("Starting daily metrics cleanup...");
            
            // 清理7天以上的数据
            long sevenDaysAgo = 7 * 24 * 60 * 60 * 1000L;
            metricsAspect.removeStaleMetrics(sevenDaysAgo);
            
            log.info("Daily metrics cleanup completed");
            
        } catch (Exception e) {
            log.error("Error during daily metrics cleanup", e);
        }
    }
}