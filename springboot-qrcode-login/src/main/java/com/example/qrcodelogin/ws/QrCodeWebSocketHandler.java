package com.example.qrcodelogin.ws;

import com.example.qrcodelogin.util.JsonUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class QrCodeWebSocketHandler extends TextWebSocketHandler {
    
    // 存储所有WebSocket连接，key为二维码ID
    private static final Map<String, WebSocketSession> SESSIONS = new ConcurrentHashMap<>();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket connection established: {}", session.getId());
    }
    
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        log.info("Received message: {}", payload);
        
        Map<String, String> msgMap = JsonUtil.fromJson(payload, Map.class);
        if (msgMap != null && msgMap.containsKey("qrCodeId")) {
            String qrCodeId = msgMap.get("qrCodeId");
            log.info("Client subscribed to QR code: {}", qrCodeId);
            
            // 将会话与二维码ID关联
            SESSIONS.put(qrCodeId, session);
            
            // 发送确认消息
            session.sendMessage(new TextMessage("{\"type\":\"CONNECTED\",\"message\":\"Connected to QR code: " + qrCodeId + "\"}"));
        }
    }
    
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("WebSocket connection closed: {}, status: {}", session.getId(), status);
        
        // 移除会话
        SESSIONS.entrySet().removeIf(entry -> entry.getValue().getId().equals(session.getId()));
    }
    
    // 向指定二维码ID的客户端发送消息
    public void sendMessage(String qrCodeId, Object message) {
        WebSocketSession session = SESSIONS.get(qrCodeId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(JsonUtil.toJson(message)));
            } catch (IOException e) {
                log.error("Failed to send message to WebSocket client", e);
            }
        }
    }
}