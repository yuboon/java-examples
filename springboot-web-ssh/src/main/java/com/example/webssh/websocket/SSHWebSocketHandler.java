package com.example.webssh.websocket;

import com.example.webssh.service.SSHConnectionManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jcraft.jsch.ChannelShell;
import com.jcraft.jsch.JSchException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SSHWebSocketHandler extends TextWebSocketHandler {
    
    @Autowired
    private SSHConnectionManager connectionManager;

    private final Map<WebSocketSession, String> sessionConnections = new ConcurrentHashMap<>();
    private final Map<WebSocketSession, String> sessionUsers = new ConcurrentHashMap<>();
    
    // 为每个WebSocket会话添加同步锁
    private final Map<WebSocketSession, Object> sessionLocks = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket连接建立: {}", session.getId());
        // 为每个会话创建同步锁
        sessionLocks.put(session, new Object());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        try {
            String payload = message.getPayload();
            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(payload);
            
            String type = jsonNode.get("type").asText();
            
            switch (type) {
                case "connect":
                    handleConnect(session, jsonNode);
                    break;
                case "command":
                    handleCommand(session, jsonNode);
                    break;
                case "resize":
                    handleResize(session, jsonNode);
                    break;
                case "disconnect":
                    handleDisconnect(session);
                    break;
                default:
                    log.warn("未知的消息类型: {}", type);
            }
        } catch (Exception e) {
            log.error("处理WebSocket消息失败", e);
            sendError(session, "处理消息失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理SSH连接请求
     */
    private void handleConnect(WebSocketSession session, JsonNode jsonNode) {
        try {
            String host = jsonNode.get("host").asText();
            int port = jsonNode.get("port").asInt(22);
            String username = jsonNode.get("username").asText();
            String password = jsonNode.get("password").asText();
            boolean enableCollaboration = jsonNode.has("enableCollaboration") && 
                                        jsonNode.get("enableCollaboration").asBoolean();
            
            // 存储用户信息
            sessionUsers.put(session, username);
            
            // 建立SSH连接
            String connectionId = connectionManager.createConnection(host, port, username, password);
            sessionConnections.put(session, connectionId);
            
            // 启动SSH通道
            ChannelShell channel = connectionManager.getChannel(connectionId);
            startSSHChannel(session, channel);

            // 发送连接成功消息
            Map<String, Object> response = new HashMap<>();
            response.put("type", "connected");
            response.put("message", "SSH连接建立成功");
            sendMessage(session, response);
            
        } catch (Exception e) {
            log.error("建立SSH连接失败", e);
            sendError(session, "连接失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理命令执行请求
     */
    private void handleCommand(WebSocketSession session, JsonNode jsonNode) {
        String connectionId = sessionConnections.get(session);
        if (connectionId == null) {
            sendError(session, "SSH连接未建立");
            return;
        }
        
        String command = jsonNode.get("command").asText();
        ChannelShell channel = connectionManager.getChannel(connectionId);
        String username = sessionUsers.get(session);
        
        if (channel != null && channel.isConnected()) {
            try {
                // 发送命令到SSH通道
                OutputStream out = channel.getOutputStream();
                out.write(command.getBytes());
                out.flush();
            } catch (IOException e) {
                log.error("发送SSH命令失败", e);
                sendError(session, "命令执行失败");
            }
        }
    }
    
    /**
     * 启动SSH通道并处理输出
     */
    private void startSSHChannel(WebSocketSession session, ChannelShell channel) {
        try {
            // 连接通道
            channel.connect();
            
            // 处理SSH输出
            InputStream in = channel.getInputStream();
            
            // 在单独的线程中读取SSH输出
            new Thread(() -> {
                byte[] buffer = new byte[4096];
                try {
                    while (channel.isConnected() && session.isOpen()) {
                        if (in.available() > 0) {
                            int len = in.read(buffer);
                            if (len > 0) {
                                String output = new String(buffer, 0, len, "UTF-8");
                                
                                // 发送给当前会话
                                sendMessage(session, Map.of(
                                    "type", "output",
                                    "data", output
                                ));
                            }
                        } else {
                            // 没有数据时短暂休眠，避免CPU占用过高
                            Thread.sleep(10);
                        }
                    }
                } catch (IOException | InterruptedException e) {
                    log.warn("SSH输出读取中断: {}", e.getMessage());
                }
            }, "SSH-Output-Reader-" + session.getId()).start();
            
        } catch (JSchException | IOException e) {
            log.error("启动SSH通道失败", e);
            sendError(session, "通道启动失败: " + e.getMessage());
        }
    }
    
    /**
     * 处理终端大小调整
     */
    private void handleResize(WebSocketSession session, JsonNode jsonNode) {
        String connectionId = sessionConnections.get(session);
        if (connectionId != null) {
            ChannelShell channel = connectionManager.getChannel(connectionId);
            if (channel != null) {
                try {
                    int cols = jsonNode.get("cols").asInt();
                    int rows = jsonNode.get("rows").asInt();
                    channel.setPtySize(cols, rows, cols * 8, rows * 16);
                } catch (Exception e) {
                    log.warn("调整终端大小失败", e);
                }
            }
        }
    }
    
    /**
     * 处理断开连接
     */
    private void handleDisconnect(WebSocketSession session) {
        String connectionId = sessionConnections.remove(session);
        String username = sessionUsers.remove(session);
        
        if (connectionId != null) {
            connectionManager.closeConnection(connectionId);
        }
        // 清理锁资源
        sessionLocks.remove(session);
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        handleDisconnect(session);
        log.info("WebSocket连接关闭: {}", session.getId());
    }
    
    /**
     * 发送消息到WebSocket客户端（线程安全）
     */
    private void sendMessage(WebSocketSession session, Object message) {
        Object lock = sessionLocks.get(session);
        if (lock == null) return;
        
        synchronized (lock) {
            try {
                if (session.isOpen()) {
                    ObjectMapper mapper = new ObjectMapper();
                    String json = mapper.writeValueAsString(message);
                    session.sendMessage(new TextMessage(json));
                }
            } catch (Exception e) {
                log.error("发送WebSocket消息失败", e);
            }
        }
    }
    
    /**
     * 发送错误消息
     */
    private void sendError(WebSocketSession session, String error) {
        sendMessage(session, Map.of(
            "type", "error",
            "message", error
        ));
    }
    
    /**
     * 从会话中获取用户信息
     */
    private String getUserFromSession(WebSocketSession session) {
        // 简化实现，实际应用中可以从session中获取认证用户信息
        return "anonymous";
    }
    
    /**
     * 从会话中获取主机信息
     */
    private String getHostFromSession(WebSocketSession session) {
        // 简化实现，实际应用中可以保存连接信息
        return "unknown";
    }
}