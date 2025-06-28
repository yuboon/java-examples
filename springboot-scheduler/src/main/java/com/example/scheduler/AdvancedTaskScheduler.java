package com.example.scheduler;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
public class AdvancedTaskScheduler {
    
    @Autowired
    private TaskScheduler taskScheduler;
    
    @Autowired
    private CronRepository cronRepository;
    
    // 维护所有调度任务
    private final Map<String, ScheduledTask> scheduledTasks = new ConcurrentHashMap<>();
    
    @Data
    @AllArgsConstructor
    private static class ScheduledTask {
        private String taskName;
        private String cron;
        private Runnable runnable;
        private ScheduledFuture<?> future;
        private boolean running;
    }
    
    /**
     * 注册一个新的定时任务
     */
    public void registerTask(String taskName, Runnable runnable) {
        // 判断任务是否已存在
        if (scheduledTasks.containsKey(taskName)) {
            throw new IllegalArgumentException("Task with name " + taskName + " already exists");
        }
        String cron = cronRepository.getCronByTaskName(taskName);
        ScheduledFuture<?> future = taskScheduler.schedule(
            runnable,
            triggerContext -> {
                // 动态获取最新的cron表达式
                String currentCron = cronRepository.getCronByTaskName(taskName);
                return new CronTrigger(currentCron).nextExecution(triggerContext);
            }
        );
        
        scheduledTasks.put(taskName, new ScheduledTask(taskName, cron, runnable, future, true));
    }
    
    /**
     * 更新任务的Cron表达式
     */
    public void updateTaskCron(String taskName, String cron) {
        // 更新数据库/配置中心中的cron表达式
        cronRepository.updateCron(taskName, cron);
        
        // 重新调度任务
        ScheduledTask task = scheduledTasks.get(taskName);
        if (task != null) {
            // 取消现有任务
            task.getFuture().cancel(false);
            
            // 创建新任务
            ScheduledFuture<?> future = taskScheduler.schedule(
                task.getRunnable(),
                triggerContext -> {
                    return new CronTrigger(cron).nextExecution(triggerContext);
                }
            );
            
            // 更新任务信息
            task.setCron(cron);
            task.setFuture(future);
        }
    }
    
    /**
     * 暂停任务
     */
    public void pauseTask(String taskName) {
        ScheduledTask task = scheduledTasks.get(taskName);
        if (task != null && task.isRunning()) {
            task.getFuture().cancel(false);
            task.setRunning(false);
        }
    }
    
    /**
     * 恢复任务
     */
    public void resumeTask(String taskName) {
        ScheduledTask task = scheduledTasks.get(taskName);
        if (task != null && !task.isRunning()) {
            ScheduledFuture<?> future = taskScheduler.schedule(
                task.getRunnable(),
                triggerContext -> {
                    String currentCron = cronRepository.getCronByTaskName(taskName);
                    return new CronTrigger(currentCron).nextExecution(triggerContext);
                }
            );
            
            task.setFuture(future);
            task.setRunning(true);
        }
    }
    
    /**
     * 删除任务
     */
    public void removeTask(String taskName) {
        ScheduledTask task = scheduledTasks.get(taskName);
        if (task != null) {
            task.getFuture().cancel(false);
            scheduledTasks.remove(taskName);
        }
    }
    
    /**
     * 获取所有任务信息
     */
    public List<Map<String, Object>> getAllTasks() {
        return scheduledTasks.values().stream()
            .map(task -> {
                Map<String, Object> map = new HashMap<>();
                map.put("taskName", task.getTaskName());
                map.put("cron", task.getCron());
                map.put("running", task.isRunning());
                return map;
            })
            .collect(Collectors.toList());
    }
}