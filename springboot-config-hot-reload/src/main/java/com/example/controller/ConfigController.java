package com.example.controller;

import com.example.AppConfig;
import com.example.config.ConfigRefreshedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class ConfigController {
    @Autowired
    private AppConfig appConfig;

    @GetMapping("/config")
    public AppConfig getConfig() {
        return appConfig; // 返回当前配置
    }

    // 监听配置刷新事件，可进行业务特殊处理
    @EventListener(ConfigRefreshedEvent.class)
    public void appConfigUpdate(ConfigRefreshedEvent event) {
        event.getChangedKeys().forEach(key -> log.info("配置项 {} 发生变化", key));
    }

}