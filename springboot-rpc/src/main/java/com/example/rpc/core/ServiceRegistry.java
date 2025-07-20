package com.example.rpc.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ServiceRegistry {
    private static final Logger logger = LoggerFactory.getLogger(ServiceRegistry.class);
    
    // 服务实例注册表：接口名 -> 服务实现实例
    private final Map<String, Object> serviceMap = new ConcurrentHashMap<>();
    
    /**
     * 注册服务实例
     */
    public void registerService(Class<?> serviceInterface, String version, Object serviceImpl) {
        String serviceName = generateServiceName(serviceInterface, version);
        serviceMap.put(serviceName, serviceImpl);
        logger.info("注册服务成功: {} -> {}", serviceName, serviceImpl.getClass().getName());
    }
    
    /**
     * 获取服务实例
     */
    public Object getService(String className, String version) {
        String serviceName = generateServiceName(className, version);
        Object service = serviceMap.get(serviceName);
        if (service == null) {
            logger.warn("未找到服务: {}", serviceName);
        }
        return service;
    }
    
    /**
     * 生成服务名称
     */
    private String generateServiceName(Class<?> serviceInterface, String version) {
        return generateServiceName(serviceInterface.getName(), version);
    }
    
    private String generateServiceName(String className, String version) {
        return className + ":" + version;
    }
    
    /**
     * 获取所有已注册的服务
     */
    public Set<String> getAllServices() {
        return new HashSet<>(serviceMap.keySet());
    }
}