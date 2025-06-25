package com.example.livestream.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Autowired
    private WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOriginPatterns("*")
                .withSockJS();
    }

    /*@Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 使用/topic和/queue前缀的目的地会路由到消息代理
        registry.enableSimpleBroker("/topic", "/queue", "/user");
        
        // 用户目标前缀
        registry.setUserDestinationPrefix("/user");
        
        // 应用前缀
        registry.setApplicationDestinationPrefixes("/app");
    }*/

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // 使用/topic和/queue前缀的目的地会路由到消息代理
        registry.enableSimpleBroker("/topic", "/queue");

        // 用户目标前缀
        registry.setUserDestinationPrefix("/user");

        // 应用前缀
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }
}