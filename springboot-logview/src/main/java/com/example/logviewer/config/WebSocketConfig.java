package com.example.logviewer.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * WebSocket 配置类
 * 用于配置 WebSocket 消息代理和端点
 */
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /**
     * 配置消息代理
     * @param config 消息代理注册器
     */
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的消息代理，用于向客户端发送消息
        config.enableSimpleBroker("/topic");
        // 设置应用程序目的地前缀，客户端发送消息时使用
        config.setApplicationDestinationPrefixes("/app");
    }

    /**
     * 注册 STOMP 端点
     * @param registry STOMP 端点注册器
     */
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册 WebSocket 端点，客户端通过此端点连接
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*") // 允许所有来源（开发环境）
                .withSockJS(); // 启用 SockJS 支持
    }
}