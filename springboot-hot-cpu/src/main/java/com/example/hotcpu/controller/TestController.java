package com.example.hotcpu.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/test")
@CrossOrigin(origins = "*")
public class TestController {

    private static final Logger logger = LoggerFactory.getLogger(TestController.class);
    private final Random random = new Random();

    @GetMapping("/cpu-intensive")
    public Map<String, Object> cpuIntensiveTask(@RequestParam(name = "iterations", defaultValue = "1000") int iterations) {
        logger.info("开始执行CPU密集型任务，迭代次数: {}", iterations);
        long startTime = System.currentTimeMillis();
        
        // 模拟CPU密集型任务
        double result = performComplexCalculation(iterations);
        
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("iterations", iterations);
        response.put("executionTimeMs", endTime - startTime);
        response.put("message", "CPU intensive task completed");
        
        logger.info("CPU intensive task completed in {}ms", endTime - startTime);
        return response;
    }

    @GetMapping("/nested-calls")
    public Map<String, Object> nestedCallsTest(@RequestParam(name = "depth", defaultValue = "10") int depth) {
        long startTime = System.currentTimeMillis();
        
        double result = recursiveMethod(depth, 1000);
        
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("depth", depth);
        response.put("executionTimeMs", endTime - startTime);
        response.put("message", "Nested calls test completed");
        
        return response;
    }

    @GetMapping("/mixed-workload")
    public Map<String, Object> mixedWorkload() throws InterruptedException {
        long startTime = System.currentTimeMillis();

        // 暴力 BigInteger 大数阶乘, 模拟耗时操作
        logger.info("开始执行大数阶乘");
        java.math.BigInteger.valueOf(1)
                .multiply(java.util.stream.LongStream.rangeClosed(2, 500_000)
                        .mapToObj(BigInteger::valueOf)
                        .reduce(BigInteger.ONE, BigInteger::multiply));
        logger.info("结束执行大数阶乘");

        // 混合工作负载：CPU + I/O
        double cpuResult = performComplexCalculation(10000000);
        Thread.sleep(100); // 模拟I/O等待
        double ioResult = performDatabaseLikeOperation();
        Thread.sleep(50);
        double finalResult = performComplexCalculation(10000000);
        
        long endTime = System.currentTimeMillis();
        
        Map<String, Object> response = new HashMap<>();
        response.put("cpuResult", cpuResult);
        response.put("ioResult", ioResult);
        response.put("finalResult", finalResult);
        response.put("executionTimeMs", endTime - startTime);
        response.put("message", "Mixed workload completed");
        
        return response;
    }

    private double performComplexCalculation(int iterations) {
        double result = 0.0;
        for (int i = 0; i < iterations; i++) {
            String id = UUID.randomUUID().toString();
            result += Math.sin(i) * Math.cos(i) * Math.sqrt(i + 1);
            result += fibonacci(i % 20); // 递归调用
            
            // 模拟一些字符串操作
            String temp = "test" + i;
            temp = temp.toUpperCase().toLowerCase();
            result += temp.hashCode() % 100;
        }
        return result;
    }

    private double recursiveMethod(int depth, int workSize) {
        if (depth <= 0) {
            return performBasicCalculation(workSize);
        }
        
        double result1 = performBasicCalculation(workSize / 2);
        double result2 = recursiveMethod(depth - 1, workSize / 2);
        
        return result1 + result2;
    }

    private double performBasicCalculation(int size) {
        double result = 0.0;
        for (int i = 0; i < size; i++) {
            result += Math.log(i + 1) * Math.exp(i * 0.001);
        }
        return result;
    }

    private double performDatabaseLikeOperation() {
        // 模拟数据库查询类的操作
        double result = 0.0;
        for (int i = 0; i < 100; i++) {
            result += queryLikeOperation(i);
        }
        return result;
    }

    private double queryLikeOperation(int id) {
        // 模拟查询操作
        return Math.random() * id + processRecord(id);
    }

    private double processRecord(int id) {
        return Math.pow(id, 2) + Math.sqrt(id);
    }

    private long fibonacci(int n) {
        if (n <= 1) return n;
        return fibonacci(n - 1) + fibonacci(n - 2);
    }

    @GetMapping("/long-running")
    public Map<String, Object> longRunningTask(@RequestParam(name = "duration", defaultValue = "5000") int durationMs) {
        logger.info("开始执行长时间运行任务，持续时间: {}ms", durationMs);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + durationMs;
        
        double result = 0.0;
        int iterations = 0;
        
        // 持续运行指定时间，确保采样器能够捕获到
        while (System.currentTimeMillis() < endTime) {
            result += performLongCalculation(100);
            iterations++;
            
            // 每100次迭代检查一次时间，避免过于频繁的时间检查
            if (iterations % 100 == 0) {
                logger.debug("Long running task - iterations: {}, current result: {}", iterations, result);
            }
        }
        
        long actualDuration = System.currentTimeMillis() - startTime;
        
        Map<String, Object> response = new HashMap<>();
        response.put("result", result);
        response.put("iterations", iterations);
        response.put("requestedDurationMs", durationMs);
        response.put("actualDurationMs", actualDuration);
        response.put("message", "Long running task completed");
        
        logger.info("Long running task completed - iterations: {}, duration: {}ms", iterations, actualDuration);
        return response;
    }

    private double performLongCalculation(int size) {
        double result = 0.0;
        for (int i = 0; i < size; i++) {
            result += deepMethodCall1(i);
        }
        return result;
    }
    
    private double deepMethodCall1(int value) {
        return deepMethodCall2(value * 2) + Math.sin(value);
    }
    
    private double deepMethodCall2(int value) {
        return deepMethodCall3(value + 1) + Math.cos(value);
    }
    
    private double deepMethodCall3(int value) {
        return deepMethodCall4(value % 100) + Math.sqrt(value);
    }
    
    private double deepMethodCall4(int value) {
        // 最深层的方法调用，包含一些计算
        double result = Math.log(value + 1);
        if (value % 10 == 0) {
            result += fibonacci(value % 15); // 递归调用
        }
        return result;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("service", "test-controller");
        response.put("availableEndpoints", new String[]{
            "/test/cpu-intensive?iterations=1000",
            "/test/nested-calls?depth=10", 
            "/test/mixed-workload",
            "/test/long-running?duration=5000",
            "/test/status"
        });
        return response;
    }
}