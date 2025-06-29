package com.example.config;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.RequestLimitingHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import io.undertow.util.HeaderMap;
import io.undertow.util.HttpString;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

//@Configuration
public class UndertowProxyConfig {

    @Bean
    @ConditionalOnProperty(name = "user.enabled", havingValue = "false", matchIfMissing = true)
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> undertowProxyCustomizer() {
        return factory -> factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            deploymentInfo.addInitialHandlerChainWrapper(handler -> {
                PathHandler pathHandler = Handlers.path(handler);

                // 配置代理路由
                HttpHandler userHandle = createProxyClient("http://127.0.0.1:8081/user");
                //HttpHandler handler2 = createProxyClient("http://127.0.0.2:8081/user/users2");

                userHandle = secureProxyHandler(userHandle);
                userHandle = createRateLimitingHandler(userHandle);

                // 添加路由规则
                pathHandler.addPrefixPath("/user", userHandle);
                //pathHandler.addPrefixPath("/user/users2", handler2);

                return pathHandler;
            });
        });
    }
    
    private HttpHandler createProxyClient(String targetUrl) {
        try {
            URI uri = new URI(targetUrl);
            LoadBalancingProxyClient proxyClient = new LoadBalancingProxyClient();
            proxyClient.addHost(uri);
            proxyClient
                    .setConnectionsPerThread(20)
                    .setMaxQueueSize(10)
                    .setSoftMaxConnectionsPerThread(20)
                    .setProblemServerRetry(5)
                    .setTtl(30000);

            return ProxyHandler.builder()
                    .setProxyClient(proxyClient)
                    .setMaxRequestTime(30000)
                    .setRewriteHostHeader(false)
                    .setReuseXForwarded(true)
                    .build();

        } catch (Exception e) {
            throw new RuntimeException("创建代理客户端失败", e);
        }
    }

    private HttpHandler secureProxyHandler(HttpHandler proxyHandler) {
        return exchange -> {
            // 移除敏感头部
            HeaderMap headers = exchange.getRequestHeaders();
            headers.remove("X-Forwarded-Server");

            // 添加安全头部
            exchange.getResponseHeaders().add(new HttpString("X-XSS-Protection"), "1; mode=block");
            exchange.getResponseHeaders().add(new HttpString("X-Content-Type-Options"), "nosniff");
            exchange.getResponseHeaders().add(new HttpString("X-Frame-Options"), "DENY");

            // 添加代理信息
            headers.add(new HttpString("X-Forwarded-For"), exchange.getSourceAddress().getAddress().getHostAddress());
            headers.add(new HttpString("X-Forwarded-Proto"), exchange.getRequestScheme());
            headers.add(new HttpString("X-Forwarded-Host"), exchange.getHostName());

            proxyHandler.handleRequest(exchange);
        };
    }

    private HttpHandler createRateLimitingHandler(HttpHandler next) {
        // 根据实际情况调整
        return new RequestLimitingHandler(1,1,next);
    }

}