package com.example.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Date;

@Component
public class ScheduledTasks {
    
    @Scheduled(cron = "0/5 * * * * ?")  // 每5分钟执行一次
    public void executeTask() {
        System.out.println("定时任务执行，时间：" + new Date());
    }
}