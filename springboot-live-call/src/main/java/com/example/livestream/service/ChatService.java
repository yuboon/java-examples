package com.example.livestream.service;

import com.example.livestream.model.ChatMessage;
import com.example.livestream.model.LiveRoom;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class ChatService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private LiveRoomService liveRoomService;

    // 内存中存储每个直播间的最近消息
    private final Map<String, List<ChatMessage>> roomMessages = new ConcurrentHashMap<>();
    private final int MAX_MESSAGES_PER_ROOM = 100;

    public void sendMessage(ChatMessage message) {
        LiveRoom room = liveRoomService.getLiveRoom(message.getRoomId());
        if (room == null || !room.isActive()) {
            return;
        }

        message.setTimestamp(LocalDateTime.now());
        
        // 将消息广播到直播间
        messagingTemplate.convertAndSend("/topic/room/" + message.getRoomId(), message);
        
        // 存储消息
        roomMessages.computeIfAbsent(message.getRoomId(), k -> new ArrayList<>()).add(message);
        
        // 限制消息数量
        List<ChatMessage> messages = roomMessages.get(message.getRoomId());
        if (messages.size() > MAX_MESSAGES_PER_ROOM) {
            messages.remove(0);
        }
        
        log.info("Message sent in room {}: {}", message.getRoomId(), message.getContent());
    }

    public List<ChatMessage> getRecentMessages(String roomId) {
        return roomMessages.getOrDefault(roomId, new ArrayList<>());
    }

    public void sendJoinMessage(String roomId, String username) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSender("System");
        message.setContent(username + " 加入了直播间");
        message.setTimestamp(LocalDateTime.now());
        message.setType(ChatMessage.MessageType.JOIN);
        
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }

    public void sendLeaveMessage(String roomId, String username) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSender("System");
        message.setContent(username + " 离开了直播间");
        message.setTimestamp(LocalDateTime.now());
        message.setType(ChatMessage.MessageType.LEAVE);
        
        messagingTemplate.convertAndSend("/topic/room/" + roomId, message);
    }

    public void sendSignalingMessage(String roomId, String sender, String receiver, String content) {
        ChatMessage message = new ChatMessage();
        message.setRoomId(roomId);
        message.setSender(sender);
        message.setContent(content);
        message.setTimestamp(LocalDateTime.now());
        message.setType(ChatMessage.MessageType.SIGNAL);
        
        // 发送给特定用户
        messagingTemplate.convertAndSendToUser(
                receiver,
                "/queue/signal",
                message);
        
        log.info("Signal sent from {} to {} in room {}", sender, receiver, roomId);
    }
}