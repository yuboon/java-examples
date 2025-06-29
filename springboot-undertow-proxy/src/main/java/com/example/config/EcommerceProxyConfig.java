package com.example.config;

import io.undertow.Handlers;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.proxy.LoadBalancingProxyClient;
import io.undertow.server.handlers.proxy.ProxyHandler;
import org.springframework.boot.web.embedded.undertow.UndertowServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.net.URI;

@Configuration
public class EcommerceProxyConfig {

    @Bean
    public WebServerFactoryCustomizer<UndertowServletWebServerFactory> ecommerceProxyCustomizer() {
        return factory -> factory.addDeploymentInfoCustomizers(deploymentInfo -> {
            deploymentInfo.addInitialHandlerChainWrapper(handler -> {
                PathHandler pathHandler = Handlers.path(handler);
                try {
                    // 用户服务代理
                    LoadBalancingProxyClient userServiceClient = new LoadBalancingProxyClient();
                    userServiceClient.addHost(new URI("http://user-service-1:8080/api/users"));
                    userServiceClient.addHost(new URI("http://user-service-2:8080/api/users"));

                    // 商品服务代理
                    LoadBalancingProxyClient productServiceClient = new LoadBalancingProxyClient();
                    productServiceClient.addHost(new URI("http://product-service-1:8080/api/products"));
                    productServiceClient.addHost(new URI("http://product-service-2:8080/api/products"));

                    // 订单服务代理
                    LoadBalancingProxyClient orderServiceClient = new LoadBalancingProxyClient();
                    orderServiceClient.addHost(new URI("http://order-service-1:8080/api/orders"));
                    orderServiceClient.addHost(new URI("http://order-service-2:8080/api/orders"));

                    // 路由规则
                    pathHandler.addPrefixPath("/api/users", createProxyHandler(userServiceClient));
                    pathHandler.addPrefixPath("/api/products", createProxyHandler(productServiceClient));
                    pathHandler.addPrefixPath("/api/orders", createProxyHandler(orderServiceClient));

                    return pathHandler;
                }catch (Exception e){
                    throw new RuntimeException(e);
                }
            });
        });
    }
    
    private HttpHandler createProxyHandler(LoadBalancingProxyClient client) {
        return ProxyHandler.builder()
                .setProxyClient(client)
                .setMaxRequestTime(30000)
                .setRewriteHostHeader(true)
                .build();
    }
}