package com.example.scheduler;

import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class CronRepository {
    
    // 这里可以连接数据库或配置中心，本例使用内存存储简化演示
    private final Map<String, String> cronMap = new ConcurrentHashMap<>();
    
    public CronRepository() {
        // 设置初始值
        cronMap.put("sampleTask", "0/10 * * * * ?");  // 默认每分钟执行一次
    }
    
    public String getCronByTaskName(String taskName) {
        return cronMap.getOrDefault(taskName, "0 0/1 * * * ?");
    }
    
    public void updateCron(String taskName, String cron) {
        // 验证cron表达式的有效性
        try {
            new CronTrigger(cron);
            cronMap.put(taskName, cron);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Invalid cron expression: " + cron, e);
        }
    }
}