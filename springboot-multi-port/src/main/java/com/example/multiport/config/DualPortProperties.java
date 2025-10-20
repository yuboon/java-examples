package com.example.multiport.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 双端口配置属性类
 */
@Data
@Component
@ConfigurationProperties(prefix = "dual.port")
public class DualPortProperties {

    /**
     * 用户端端口，默认8082
     */
    private int userPort = 8082;

    /**
     * 管理端端口，默认8083
     */
    private int adminPort = 8083;
}