package com.example;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app") // 绑定配置前缀
public class AppConfig {

    private int timeout = 3000; // 默认超时时间3秒
    private int maxRetries = 2; // 默认重试次数2次

}