package com.example.config;

import org.springframework.context.ApplicationEvent;

import java.util.Set;

/**
 * 自定义配置刷新事件
 */
public class ConfigRefreshedEvent extends ApplicationEvent {
    // 存储变化的配置键（可选，方便业务判断哪些配置变了）
    private final Set<String> changedKeys;

    public ConfigRefreshedEvent(Object source, Set<String> changedKeys) {
        super(source);
        this.changedKeys = changedKeys;
    }

    // 获取变化的配置键
    public Set<String> getChangedKeys() {
        return changedKeys;
    }
}