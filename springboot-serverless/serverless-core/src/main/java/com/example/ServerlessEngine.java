package com.example;

import cn.hutool.core.io.FileUtil;
import com.example.core.FunctionManager;
import com.example.trigger.TimerTrigger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.HashMap;
import java.util.Map;

/**
 * Serverless引擎启动类
 */
@SpringBootApplication
@EnableScheduling
public class ServerlessEngine implements CommandLineRunner {
    
    @Autowired
    private FunctionManager functionManager;
    
    @Autowired
    private TimerTrigger timerTrigger;
    
    public static void main(String[] args) {
        FileUtil.writeBytes("123".getBytes(),"functions/function.txt");
        SpringApplication.run(ServerlessEngine.class, args);
    }
    
    @Override
    public void run(String... args) throws Exception {
        System.out.println("=== Serverless Engine Started ===");
        
        // 注册示例函数
        registerDemoFunctions();
        
        // 注册示例定时任务
        registerDemoTimerTasks();
        
        System.out.println("=== Demo Functions and Tasks Registered ===");
        System.out.println("API available at: http://localhost:8080/serverless");
    }
    
    /**
     * 注册演示函数
     */
    private void registerDemoFunctions() {
        // 注册Hello World函数
        functionManager.registerFunction(
            "hello-world",
            "functions/demo-function.jar",
            "com.example.functions.HelloWorldFunction"
        );
        
        // 注册用户服务函数
        Map<String, Object> userEnv = new HashMap<>();
        userEnv.put("DB_URL", "jdbc:h2:mem:testdb");
        userEnv.put("MAX_USERS", "1000");
        
        functionManager.registerFunction(
            "user-service",
            "functions/user-function.jar",
            "com.example.functions.UserServiceFunction",
            60000, // 60秒超时
            userEnv
        );
    }
    
    /**
     * 注册演示定时任务
     */
    private void registerDemoTimerTasks() {
        // 注册清理任务
        timerTrigger.registerTimerTask(
            "cleanup-task",
            "user-service",
            "0 0 2 * * ?" // 每天凌晨2点执行
        );
        
        // 注册健康检查任务
        timerTrigger.registerTimerTask(
            "health-check",
            "hello-world",
            "0/10 * * * * ?" // 每10秒执行一次
        );
    }
}