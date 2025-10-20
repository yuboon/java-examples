package com.example.multiport.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.Connector;
import org.apache.coyote.http11.Http11NioProtocol;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 双端口配置类
 * 配置Tomcat监听两个不同端口
 */
@Slf4j
@Configuration
public class DualPortConfiguration {

    @Bean
    public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory>
            webServerFactoryCustomizer(DualPortProperties properties) {

        return factory -> {
            if (factory instanceof TomcatServletWebServerFactory) {
                TomcatServletWebServerFactory tomcatFactory = (TomcatServletWebServerFactory)factory;
                // 添加管理端端口连接器
                tomcatFactory.addAdditionalTomcatConnectors(
                    createAdminConnector(properties.getAdminPort())
                );
                log.info("配置双端口：用户端端口={}, 管理端端口={}",
                    properties.getUserPort(), properties.getAdminPort());
            }
        };
    }

    /**
     * 创建管理端端口连接器
     */
    private Connector createAdminConnector(int port) {
        Connector connector = new Connector("org.apache.coyote.http11.Http11NioProtocol");
        connector.setPort(port);
        connector.setProperty("connectionTimeout", "20000");
        connector.setProperty("maxThreads", "200");
        connector.setProperty("minSpareThreads", "10");

        // 设置协议处理器
        Http11NioProtocol protocol = (Http11NioProtocol) connector.getProtocolHandler();
        protocol.setConnectionTimeout(20000);
        protocol.setMaxThreads(200);
        protocol.setMinSpareThreads(10);

        log.info("创建管理端连接器，端口: {}", port);
        return connector;
    }
}