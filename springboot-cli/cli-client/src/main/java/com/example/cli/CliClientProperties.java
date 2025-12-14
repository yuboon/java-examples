package com.example.cli;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CLI客户端配置属性
 */
@Component
@ConfigurationProperties(prefix = "cli.client")
public class CliClientProperties {

    /**
     * 服务端URL
     */
    private String serverUrl = "http://localhost:8080";

    /**
     * 请求超时时间（毫秒）
     */
    private long timeout = 30000;

    /**
     * 连接超时时间（毫秒）
     */
    private long connectTimeout = 5000;

    /**
     * 是否启用请求重试
     */
    private boolean retryEnabled = true;

    /**
     * 最大重试次数
     */
    private int maxRetries = 3;

    public String getServerUrl() {
        return serverUrl;
    }

    public void setServerUrl(String serverUrl) {
        this.serverUrl = serverUrl;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getConnectTimeout() {
        return connectTimeout;
    }

    public void setConnectTimeout(long connectTimeout) {
        this.connectTimeout = connectTimeout;
    }

    public boolean isRetryEnabled() {
        return retryEnabled;
    }

    public void setRetryEnabled(boolean retryEnabled) {
        this.retryEnabled = retryEnabled;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }
}