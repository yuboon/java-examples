package com.example.webssh.service;

import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SSHConnectionManager {
    
    private final Map<String, Session> connections = new ConcurrentHashMap<>();
    private final Map<String, ChannelShell> channels = new ConcurrentHashMap<>();
    
    /**
     * 建立SSH连接
     */
    public String createConnection(String host, int port, String username, String password) {
        try {
            JSch jsch = new JSch();
            Session session = jsch.getSession(username, host, port);
            
            // 配置连接参数
            Properties config = new Properties();
            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "password");
            session.setConfig(config);
            session.setPassword(password);
            
            // 建立连接
            session.connect(30000); // 30秒超时
            
            // 创建Shell通道
            ChannelShell channel = (ChannelShell) session.openChannel("shell");
            channel.setPty(true);
            channel.setPtyType("xterm", 80, 24, 640, 480);
            
            // 生成连接ID
            String connectionId = UUID.randomUUID().toString();
            
            // 保存连接和通道
            connections.put(connectionId, session);
            channels.put(connectionId, channel);
            
            log.info("SSH连接建立成功: {}@{}:{}", username, host, port);
            return connectionId;
            
        } catch (JSchException e) {
            log.error("SSH连接失败: {}", e.getMessage());
            throw new RuntimeException("SSH连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取SSH通道
     */
    public ChannelShell getChannel(String connectionId) {
        return channels.get(connectionId);
    }
    
    /**
     * 获取SSH会话
     */
    public Session getSession(String connectionId) {
        return connections.get(connectionId);
    }
    
    /**
     * 关闭SSH连接
     */
    public void closeConnection(String connectionId) {
        ChannelShell channel = channels.remove(connectionId);
        if (channel != null && channel.isConnected()) {
            channel.disconnect();
        }
        
        Session session = connections.remove(connectionId);
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
        
        log.info("SSH连接已关闭: {}", connectionId);
    }
    
    /**
     * 检查连接状态
     */
    public boolean isConnected(String connectionId) {
        Session session = connections.get(connectionId);
        return session != null && session.isConnected();
    }
}