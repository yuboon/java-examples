package com.example.trigger;

import com.example.core.ExecutionEngine;
import com.example.model.ExecutionResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 定时触发器
 * 支持cron表达式定时触发函数
 */
@Component
public class TimerTrigger {
    
    @Autowired
    private ExecutionEngine executionEngine;
    
    // 定时任务注册表
    private final Map<String, TimerTask> timerTasks = new ConcurrentHashMap<>();
    
    /**
     * 定时任务定义
     */
    public static class TimerTask {
        private String name;
        private String functionName;
        private String cronExpression;
        private boolean enabled;
        private LocalDateTime lastExecution;
        private LocalDateTime nextExecution;
        private long executionCount;
        
        public TimerTask(String name, String functionName, String cronExpression) {
            this.name = name;
            this.functionName = functionName;
            this.cronExpression = cronExpression;
            this.enabled = true;
            this.executionCount = 0;
        }
        
        // Getter和Setter方法
        public String getName() { return name; }
        public String getFunctionName() { return functionName; }
        public String getCronExpression() { return cronExpression; }
        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public LocalDateTime getLastExecution() { return lastExecution; }
        public void setLastExecution(LocalDateTime lastExecution) { this.lastExecution = lastExecution; }
        public LocalDateTime getNextExecution() { return nextExecution; }
        public void setNextExecution(LocalDateTime nextExecution) { this.nextExecution = nextExecution; }
        public long getExecutionCount() { return executionCount; }
        public void incrementExecutionCount() { this.executionCount++; }
    }
    
    /**
     * 注册定时任务
     */
    public void registerTimerTask(String taskName, String functionName, String cronExpression) {
        TimerTask task = new TimerTask(taskName, functionName, cronExpression);
        timerTasks.put(taskName, task);
        System.out.println("Timer task registered: " + taskName + " -> " + functionName + " (" + cronExpression + ")");
    }
    
    /**
     * 移除定时任务
     */
    public void removeTimerTask(String taskName) {
        if (timerTasks.remove(taskName) != null) {
            System.out.println("Timer task removed: " + taskName);
        }
    }
    
    /**
     * 启用/禁用定时任务
     */
    public void setTimerTaskEnabled(String taskName, boolean enabled) {
        TimerTask task = timerTasks.get(taskName);
        if (task != null) {
            task.setEnabled(enabled);
            System.out.println("Timer task " + taskName + " " + (enabled ? "enabled" : "disabled"));
        }
    }
    
    /**
     * 获取所有定时任务
     */
    public Map<String, TimerTask> getAllTimerTasks() {
        return new HashMap<>(timerTasks);
    }
    
    /**
     * 手动执行定时任务
     */
    public ExecutionResult executeTimerTask(String taskName) {
        TimerTask task = timerTasks.get(taskName);
        if (task == null) {
            throw new IllegalArgumentException("Timer task not found: " + taskName);
        }
        
        return executeTask(task);
    }
    
    /**
     * 定时执行 - 每分钟检查一次
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void checkAndExecuteTimerTasks() {
        LocalDateTime now = LocalDateTime.now();
        
        timerTasks.values().stream()
            .filter(TimerTask::isEnabled)
            .forEach(task -> {
                // 这里简化处理，实际应该解析cron表达式
                // 为了演示，我们每5分钟执行一次
                if (task.getLastExecution() == null || 
                    task.getLastExecution().isBefore(now.minusMinutes(5))) {
                    
                    executeTask(task);
                }
            });
    }
    
    /**
     * 执行定时任务
     */
    private ExecutionResult executeTask(TimerTask task) {
        // 构建输入参数
        Map<String, Object> input = new HashMap<>();
        
        Map<String, Object> timerInfo = new HashMap<>();
        timerInfo.put("taskName", task.getName());
        timerInfo.put("cronExpression", task.getCronExpression());
        timerInfo.put("executionTime", LocalDateTime.now().toString());
        timerInfo.put("executionCount", task.getExecutionCount());
        
        input.put("timer", timerInfo);
        
        // 执行函数
        ExecutionResult result = executionEngine.invoke(task.getFunctionName(), input);
        
        // 更新任务信息
        task.setLastExecution(LocalDateTime.now());
        task.incrementExecutionCount();
        
        System.out.println("Timer task executed: " + task.getName() + 
                          " -> " + task.getFunctionName() + 
                          ", success: " + result.isSuccess());
        
        return result;
    }
}