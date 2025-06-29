package com.example.config;

import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "user.enabled", havingValue = "false", matchIfMissing = true)
@Slf4j
public class BackendHealthMonitor {
    
    private final LoadBalancingProxyClient loadBalancer;
    private final List<URI> backendServers;
    private final RestTemplate restTemplate;
    
    public BackendHealthMonitor(@Value("#{'${user.backends}'.split(',')}") String[] backends,
            LoadBalancingProxyClient loadBalancer) throws URISyntaxException {
        this.loadBalancer = loadBalancer;
        this.restTemplate = new RestTemplate();
        this.backendServers = Arrays.stream(backends)
                .map(url -> {
                    try {
                        return new URI(url);
                    } catch (URISyntaxException e) {
                        throw new RuntimeException(e);
                    }
                })
                .collect(Collectors.toList());
    }
    
    @Scheduled(fixedDelay = 10000) // 每10秒检查一次
    public void checkBackendHealth() {
        for (URI server : backendServers) {
            try {
                String healthUrl = server.getScheme() + "://" + server.getHost() + ":" + server.getPort() + "/health";
                ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
                
                if (response.getStatusCode().is2xxSuccessful()) {
                    loadBalancer.addHost(server);
                    log.info("后端服务 {} 状态正常，已添加到负载均衡", server);
                } else {
                    // 服务不健康，从负载均衡器中移除
                    loadBalancer.removeHost(server);
                    log.warn("后端服务 {} 状态异常，已从负载均衡中移除", server);
                }
            } catch (Exception e) {
                // 连接异常，从负载均衡器中移除
                loadBalancer.removeHost(server);
                log.error("后端服务 {} 连接异常: {}", server, e.getMessage());
            }
        }
    }
}