package com.example.livestream.controller;

import com.example.livestream.model.ChatMessage;
import com.example.livestream.model.LiveRoom;
import com.example.livestream.service.ChatService;
import com.example.livestream.service.LiveRoomService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;
import java.util.List;
import java.util.Map;

@Controller
@Slf4j
public class WebSocketController {

    @Autowired
    private ChatService chatService;

    @Autowired
    private LiveRoomService liveRoomService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/heartbeat")
    public void processHeartbeat(String message, Principal principal) {
        if (principal != null) {
            log.debug("Received heartbeat from {}: {}", principal.getName(), message);
        }
    }

    @MessageMapping("/chat.send/{roomId}")
    public void sendMessage(@DestinationVariable String roomId, 
                           @Payload ChatMessage chatMessage,
                           Principal principal) {
        if (principal == null) return;
        
        chatMessage.setRoomId(roomId);
        chatMessage.setSender(principal.getName());
        chatMessage.setType(ChatMessage.MessageType.CHAT);
        
        chatService.sendMessage(chatMessage);
    }

    @SubscribeMapping("/room/{roomId}")
    public List<ChatMessage> subscribeRoom(@DestinationVariable String roomId, 
                                         Principal principal,
                                         SimpMessageHeaderAccessor headerAccessor) {
        if (principal == null) return null;
        
        String username = principal.getName();
        liveRoomService.joinLiveRoom(roomId, username);
        chatService.sendJoinMessage(roomId, username);
        
        return chatService.getRecentMessages(roomId);
    }

    @MessageMapping("/mic.request/{roomId}")
    public void requestMic(@DestinationVariable String roomId, Principal principal, SimpMessageHeaderAccessor headerAccessor) {
        if (principal == null) {
            log.error("Cannot request mic: user not authenticated");
            return;
        }

        String username = principal.getName();
        log.info("Received mic request from {} for room {}", username, roomId);

        // 输出当前会话的所有信息，帮助调试
        log.info("Session ID: {}", headerAccessor.getSessionId());
        log.info("User: {}", headerAccessor.getUser());
        log.info("Headers: {}", headerAccessor.toMap());

        boolean success = liveRoomService.requestMic(roomId, username);
        log.info("Mic request processing result: {}", success ? "SUCCESS" : "FAILED");

        // 如果处理失败，直接通知请求者
        if (!success) {
            messagingTemplate.convertAndSendToUser(
                    username,
                    "/queue/mic-response",
                    "Mic request failed");
        }
    }

    @MessageMapping("/mic.response/{roomId}")
    public void handleMicRequest(@DestinationVariable String roomId, 
                                @Payload Map<String, Object> payload,
                                Principal principal) {
        if (principal == null) return;
        
        String requestUsername = (String) payload.get("username");
        boolean accept = (boolean) payload.get("accept");
        
        liveRoomService.handleMicRequest(roomId, requestUsername, accept, principal.getName());
    }

    @MessageMapping("/mic.end/{roomId}")
    public void endMic(@DestinationVariable String roomId, 
                      @Payload String username,
                      Principal principal) {
        if (principal == null) return;
        
        liveRoomService.endMic(roomId, username, principal.getName());
    }

    @MessageMapping("/signal/{roomId}")
    public void handleSignal(@DestinationVariable String roomId, 
                            @Payload Map<String, String> payload,
                            Principal principal) {
        if (principal == null) return;
        
        String sender = principal.getName();
        String receiver = payload.get("receiver");
        String content = payload.get("content");
        
        chatService.sendSignalingMessage(roomId, sender, receiver, content);
    }
}