package com.example.livestream.controller;

import com.example.livestream.entity.LiveRoom;
import com.example.livestream.mapper.LiveRoomMapper;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LiveChatController {
    
    @Autowired
    private LiveRoomMapper liveRoomMapper;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private StringRedisTemplate redisTemplate;

    /**
     * 发送聊天消息
     */
    @MessageMapping("/chat/{roomId}")
    public void sendMessage(@DestinationVariable Long roomId, ChatMessage message) {
        // 检查直播间是否存在
        LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
        if (liveRoom == null || liveRoom.getStatus() != 1) {
            return;
        }
        
        // 设置消息时间
        message.setTimestamp(LocalDateTime.now());
        
        // 发送消息到订阅该直播间的所有客户端
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, message);
    }
    
    /**
     * 直播间状态变更通知
     */
    public void notifyRoomStatusChange(Long roomId, int status) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("roomId", roomId);
        payload.put("status", status);
        payload.put("timestamp", LocalDateTime.now());
        
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/status", payload);
    }
    
    /**
     * 发送直播点赞通知
     */
    @MessageMapping("/like/{roomId}")
    public void sendLike(@DestinationVariable Long roomId, Map<String, Object> payload) {
        // 检查直播间是否存在
        LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
        if (liveRoom == null || liveRoom.getStatus() != 1) {
            return;
        }
        
        // 增加点赞数
        String key = "live:room:" + roomId + ":like_count";
        Long likeCount = redisTemplate.opsForValue().increment(key);
        
        // 定期同步到数据库
        if (likeCount % 100 == 0) {  // 每100个点赞同步一次
            LiveRoom room = new LiveRoom();
            room.setId(roomId);
            room.setLikeCount(likeCount);
            liveRoomMapper.updateById(room);
        }
        
        // 添加时间戳
        payload.put("timestamp", LocalDateTime.now());
        payload.put("likeCount", likeCount);
        
        // 发送点赞通知
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/like", payload);
    }
    
    @Data
    public static class ChatMessage {
        private String username;
        private String userId;
        private String content;
        private String avatar;
        private LocalDateTime timestamp;
    }
}