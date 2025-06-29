package com.example;

import io.undertow.UndertowOptions;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.RouteParsingStrategy;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.xnio.Options;

import java.net.URI;
import java.net.URISyntaxException;

@SpringBootApplication
@EnableScheduling
public class Application {

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    @ConditionalOnProperty(name = "user.enabled", havingValue = "false", matchIfMissing = true)
    public LoadBalancingProxyClient loadBalancingProxyClient(@Value("#{'${user.backends}'.split(',')}") String[] backends) {
        LoadBalancingProxyClient loadBalancer = new LoadBalancingProxyClient();
        // 配置负载均衡策略
        loadBalancer.setConnectionsPerThread(20);
        // 添加后端服务器
        try {
            for (String backend : backends) {
                loadBalancer.addHost(new URI(backend));
                loadBalancer.setRouteParsingStrategy(RouteParsingStrategy.RANKED);
                loadBalancer
                        .setConnectionsPerThread(20)
                        .setMaxQueueSize(10)
                        .setSoftMaxConnectionsPerThread(20)
                        .setProblemServerRetry(5)
                        .setTtl(30000);
            }
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        // 设置会话亲和性（可选）
        loadBalancer.addSessionCookieName("JSESSIONID");
        return loadBalancer;
    }

    @Bean
    public UndertowServletWebServerFactory undertowFactory() {
        UndertowServletWebServerFactory factory = new UndertowServletWebServerFactory();
        factory.addBuilderCustomizers(builder -> {
            builder.setSocketOption(Options.KEEP_ALIVE, true)
                    .setSocketOption(Options.TCP_NODELAY, true)
                    .setSocketOption(Options.REUSE_ADDRESSES, true)
                    .setSocketOption(Options.BACKLOG, 128)
                    .setServerOption(UndertowOptions.MAX_ENTITY_SIZE, 16 * 1024 * 1024L)
                    .setServerOption(UndertowOptions.IDLE_TIMEOUT, 60 * 1000)
                    .setServerOption(UndertowOptions.REQUEST_PARSE_TIMEOUT, 30 * 1000)
                    .setServerOption(UndertowOptions.NO_REQUEST_TIMEOUT, 60 * 1000)
                    .setServerOption(UndertowOptions.MAX_CONCURRENT_REQUESTS_PER_CONNECTION, 200);
        });
        return factory;
    }

}