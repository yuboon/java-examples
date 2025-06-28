package com.example.scheduler;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.SchedulingConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.config.ScheduledTaskRegistrar;
import org.springframework.scheduling.support.CronTrigger;

import java.util.Date;

@Configuration
@EnableScheduling
public class DynamicScheduleConfig implements SchedulingConfigurer {
    
    @Autowired
    private CronRepository cronRepository;  // 用于存储和获取Cron表达式
    
    @Override
    public void configureTasks(ScheduledTaskRegistrar taskRegistrar) {
        taskRegistrar.setScheduler(taskScheduler());
        
        // 从数据库或配置中心获取初始Cron表达式
        String initialCron = cronRepository.getCronByTaskName("sampleTask");
        
        // 添加可动态修改的定时任务
        taskRegistrar.addTriggerTask(
            // 定时任务的执行逻辑
            () -> {
                System.out.println("动态定时任务执行，时间：" + new Date());
            },
            // 定时任务触发器，可根据需要返回下次执行时间
            triggerContext -> {
                // 每次任务触发时，重新获取Cron表达式
                String cron = cronRepository.getCronByTaskName("sampleTask");
                CronTrigger trigger = new CronTrigger(cron);
                return trigger.nextExecution(triggerContext);
            }
        );
    }
    
    @Bean
    public TaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(10);  // 设置线程池大小
        scheduler.setThreadNamePrefix("task-");
        scheduler.initialize();
        return scheduler;
    }
}