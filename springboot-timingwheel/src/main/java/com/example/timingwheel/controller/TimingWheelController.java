package com.example.timingwheel.controller;

import com.example.timingwheel.model.TimerTaskWrapper;
import com.example.timingwheel.service.TimingWheelService;
import com.example.timingwheel.util.TimingWheel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 时间轮控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/timingwheel")
@CrossOrigin(origins = "*")
public class TimingWheelController {

    @Autowired
    private TimingWheelService timingWheelService;

    /**
     * 获取时间轮统计信息
     */
    @GetMapping("/stats")
    public ResponseEntity<TimingWheel.TimingWheelStats> getStats() {
        TimingWheel.TimingWheelStats stats = timingWheelService.getStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取任务执行统计
     */
    @GetMapping("/execution-stats")
    public ResponseEntity<TimingWheelService.TaskExecutionStats> getExecutionStats() {
        TimingWheelService.TaskExecutionStats stats = timingWheelService.getExecutionStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 获取所有活跃任务
     */
    @GetMapping("/tasks")
    public ResponseEntity<List<TimerTaskWrapper>> getActiveTasks() {
        List<TimerTaskWrapper> tasks = timingWheelService.getActiveTasks();
        return ResponseEntity.ok(tasks);
    }

    /**
     * 获取特定任务信息
     */
    @GetMapping("/tasks/{taskId}")
    public ResponseEntity<TimerTaskWrapper> getTask(@PathVariable String taskId) {
        TimerTaskWrapper task = timingWheelService.getTaskInfo(taskId);
        if (task != null) {
            return ResponseEntity.ok(task);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 创建示例任务
     */
    @PostMapping("/tasks/sample")
    public ResponseEntity<Map<String, Object>> createSampleTask(@RequestBody Map<String, Object> request) {
        try {
            String type = (String) request.getOrDefault("type", "simple");
            long delay = ((Number) request.getOrDefault("delay", 1000)).longValue();

            String taskId = timingWheelService.createSampleTask(type, delay);

            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("type", type);
            response.put("delay", delay);
            response.put("message", "Task created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create sample task", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 批量创建任务
     */
    @PostMapping("/tasks/batch")
    public ResponseEntity<Map<String, Object>> createBatchTasks(@RequestBody Map<String, Object> request) {
        try {
            int count = (Integer) request.getOrDefault("count", 10);
            long minDelay = ((Number) request.getOrDefault("minDelay", 1000)).longValue();
            long maxDelay = ((Number) request.getOrDefault("maxDelay", 10000)).longValue();

            List<String> taskIds = timingWheelService.createBatchTasks(count, minDelay, maxDelay);

            Map<String, Object> response = new HashMap<>();
            response.put("taskIds", taskIds);
            response.put("count", taskIds.size());
            response.put("message", "Batch tasks created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create batch tasks", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 取消任务
     */
    @DeleteMapping("/tasks/{taskId}")
    public ResponseEntity<Map<String, Object>> cancelTask(@PathVariable String taskId) {
        boolean cancelled = timingWheelService.cancelTask(taskId);
        Map<String, Object> response = new HashMap<>();
        response.put("taskId", taskId);
        response.put("cancelled", cancelled);
        response.put("message", cancelled ? "Task cancelled successfully" : "Task not found or already completed");

        if (cancelled) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * 清理已完成的任务
     */
    @PostMapping("/cleanup")
    public ResponseEntity<Map<String, Object>> cleanupTasks() {
        int removedCount = timingWheelService.cleanupCompletedTasks();
        Map<String, Object> response = new HashMap<>();
        response.put("removedCount", removedCount);
        response.put("message", "Cleaned up " + removedCount + " completed tasks");

        return ResponseEntity.ok(response);
    }

    /**
     * 创建自定义任务
     */
    @PostMapping("/tasks/custom")
    public ResponseEntity<Map<String, Object>> createCustomTask(@RequestBody Map<String, Object> request) {
        try {
            String description = (String) request.getOrDefault("description", "Custom task");
            long delay = ((Number) request.getOrDefault("delay", 1000)).longValue();
            String action = (String) request.getOrDefault("action", "log");

            String taskId = timingWheelService.scheduleTask(() -> {
                switch (action.toLowerCase()) {
                    case "log":
                        log.info("Custom task executed: {} at {}", description, java.time.LocalDateTime.now());
                        break;
                    case "calc":
                        performCalculation(description);
                        break;
                    case "sleep":
                        performSleep(description);
                        break;
                    default:
                        log.info("Unknown action: {} for task: {}", action, description);
                }
            }, delay, description);

            Map<String, Object> response = new HashMap<>();
            response.put("taskId", taskId);
            response.put("description", description);
            response.put("delay", delay);
            response.put("action", action);
            response.put("message", "Custom task created successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to create custom task", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 压力测试
     */
    @PostMapping("/stress-test")
    public ResponseEntity<Map<String, Object>> stressTest(@RequestBody Map<String, Object> request) {
        try {
            int taskCount = (Integer) request.getOrDefault("taskCount", 1000);
            long minDelay = ((Number) request.getOrDefault("minDelay", 100)).longValue();
            long maxDelay = ((Number) request.getOrDefault("maxDelay", 5000)).longValue();

            long startTime = System.currentTimeMillis();
            List<String> taskIds = timingWheelService.createBatchTasks(taskCount, minDelay, maxDelay);
            long endTime = System.currentTimeMillis();

            Map<String, Object> response = new HashMap<>();
            response.put("taskCount", taskIds.size());
            response.put("creationTime", endTime - startTime);
            response.put("throughput", taskIds.size() * 1000.0 / (endTime - startTime));
            response.put("message", "Stress test completed successfully");

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("Failed to perform stress test", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/system-info")
    public ResponseEntity<Map<String, Object>> getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        Map<String, Object> info = new HashMap<>();
        info.put("availableProcessors", runtime.availableProcessors());
        info.put("freeMemory", runtime.freeMemory());
        info.put("totalMemory", runtime.totalMemory());
        info.put("maxMemory", runtime.maxMemory());
        info.put("usedMemory", runtime.totalMemory() - runtime.freeMemory());
        info.put("currentTime", java.time.LocalDateTime.now());

        return ResponseEntity.ok(info);
    }

    private void performCalculation(String description) {
        long result = 0;
        for (int i = 0; i < 1000000; i++) {
            result += i;
        }
        log.info("Calculation task '{}' completed, result: {}", description, result);
    }

    private void performSleep(String description) {
        try {
            Thread.sleep(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("Sleep task '{}' completed", description);
    }
}