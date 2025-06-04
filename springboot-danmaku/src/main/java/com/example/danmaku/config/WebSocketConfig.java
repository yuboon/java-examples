package com.example.danmaku.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // 启用简单的消息代理，用于将消息返回给客户端
        config.enableSimpleBroker("/topic");
        // 设置应用程序前缀
        config.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // 注册STOMP端点，客户端通过这个端点连接到WebSocket服务器
        registry.addEndpoint("/ws-danmaku")
                .setAllowedOriginPatterns("*")
                .withSockJS(); // 启用SockJS fallback选项
    }
}