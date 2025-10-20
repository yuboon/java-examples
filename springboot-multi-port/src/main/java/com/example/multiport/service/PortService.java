package com.example.multiport.service;

import com.example.multiport.config.DualPortProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * 端口服务类
 * 统一管理端口配置，避免硬编码
 */
@Slf4j
@Service
public class PortService {

    @Autowired
    private DualPortProperties portProperties;

    /**
     * 获取用户端端口
     */
    public int getUserPort() {
        return portProperties.getUserPort();
    }

    /**
     * 获取管理端端口
     */
    public int getAdminPort() {
        return portProperties.getAdminPort();
    }

    /**
     * 获取用户端端口号（字符串）
     */
    public String getUserPortString() {
        return String.valueOf(getUserPort());
    }

    /**
     * 获取管理端端口号（字符串）
     */
    public String getAdminPortString() {
        return String.valueOf(getAdminPort());
    }

    /**
     * 判断是否为用户端端口
     */
    public boolean isUserPort(int port) {
        return port == getUserPort();
    }

    /**
     * 判断是否为管理端端口
     */
    public boolean isAdminPort(int port) {
        return port == getAdminPort();
    }

    /**
     * 判断是否为用户端端口（字符串比较）
     */
    public boolean isUserPort(String port) {
        return getUserPortString().equals(port);
    }

    /**
     * 判断是否为管理端端口（字符串比较）
     */
    public boolean isAdminPort(String port) {
        return getAdminPortString().equals(port);
    }

    /**
     * 根据端口获取服务类型
     */
    public String getServiceType(int port) {
        return isUserPort(port) ? "USER" : "ADMIN";
    }

    /**
     * 根据端口获取服务类型（中文）
     */
    public String getServiceTypeChinese(int port) {
        return isUserPort(port) ? "用户端" : "管理端";
    }

    /**
     * 根据端口获取服务类型标识
     */
    public String getServiceTypePrefix(int port) {
        return isUserPort(port) ? "user" : "admin";
    }

    /**
     * 获取用户端URL前缀
     */
    public String getUserUrlPrefix() {
        return "http://localhost:" + getUserPort();
    }

    /**
     * 获取管理端URL前缀
     */
    public String getAdminUrlPrefix() {
        return "http://localhost:" + getAdminPort();
    }

    /**
     * 获取用户端健康检查URL
     */
    public String getUserHealthUrl() {
        return getUserUrlPrefix() + "/health/user";
    }

    /**
     * 获取管理端健康检查URL
     */
    public String getAdminHealthUrl() {
        return getAdminUrlPrefix() + "/health/admin";
    }

    /**
     * 获取允许的CORS源地址
     */
    public String[] getAllowedOrigins() {
        return new String[]{
            getUserUrlPrefix(),
            getAdminUrlPrefix(),
            "http://127.0.0.1:" + getUserPort(),
            "http://127.0.0.1:" + getAdminPort()
        };
    }

    /**
     * 记录端口配置信息
     */
    public void logPortConfiguration() {
        log.info("端口服务配置 - 用户端端口: {}, 管理端端口: {}",
                getUserPort(), getAdminPort());
        log.info("用户端URL: {}", getUserUrlPrefix());
        log.info("管理端URL: {}", getAdminUrlPrefix());
    }
}