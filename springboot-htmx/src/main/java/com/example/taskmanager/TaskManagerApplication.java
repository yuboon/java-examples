package com.example.taskmanager;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootApplication
@RestController
@RequestMapping("/api/tasks")
public class TaskManagerApplication {

    // 内存中的任务存储
    private List<Task> tasks = new ArrayList<>();

    public static void main(String[] args) {
        SpringApplication.run(TaskManagerApplication.class, args);
    }

    // 任务模型
    record Task(String id, String title, boolean completed) {}

    // 获取所有任务
    @GetMapping
    public List<Task> getAllTasks() {
        return tasks;
    }

    // 创建新任务
    @PostMapping
    public Task createTask(@RequestBody Task task) {
        Task newTask = new Task(UUID.randomUUID().toString(), task.title(), false);
        tasks.add(newTask);
        return newTask;
    }

    // 更新任务状态
    @PutMapping("/{id}/toggle")
    public Task toggleTask(@PathVariable String id) {
        Task task = tasks.stream()
                .filter(t -> t.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Task not found"));
        
        Task updatedTask = new Task(task.id(), task.title(), !task.completed());
        tasks.remove(task);
        tasks.add(updatedTask);
        return updatedTask;
    }

    // 删除任务
    @DeleteMapping("/{id}")
    public void deleteTask(@PathVariable String id) {
        tasks.removeIf(t -> t.id().equals(id));
    }
}