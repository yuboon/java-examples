package com.example.logviewer;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot日志查看系统主启动类
 * 
 * @author example
 * @version 1.0.0
 */
@SpringBootApplication
@Slf4j
public class LogViewerApplication {

    public static void main(String[] args) {
        SpringApplication.run(LogViewerApplication.class, args);
        System.out.println("\n==================================");
        System.out.println("日志查看系统启动成功！");
        System.out.println("访问地址: http://localhost:8080");
        System.out.println("==================================");

        // 模拟日志输出
        ThreadUtil.execute(() -> {
            while(true){
                ThreadUtil.sleep(3000);
                logTest();
            }
        });
    }

    public static void logTest() {
        int random = RandomUtil.randomInt(1,4);
        if(random == 1){
            log.info("这是一条info日志");
        }else if(random == 2){
            log.warn("这是一条warn日志");
        }else{
            log.error("这是一条error日志");
        }
    }

}