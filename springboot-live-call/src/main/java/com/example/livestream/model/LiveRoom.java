package com.example.livestream.model;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;

@Data
public class LiveRoom {
    private String roomId;
    private String title;
    private String broadcaster;
    private LocalDateTime createdTime;
    private boolean active;
    
    // 连麦用户列表
    private Set<String> activeMicUsers = new CopyOnWriteArraySet<>();
    
    // 观众列表
    private Set<String> audiences = new CopyOnWriteArraySet<>();
    
    // 连麦申请列表
    private Map<String, MicRequest> micRequests = new ConcurrentHashMap<>();
    
    @Data
    public static class MicRequest {
        private String username;
        private LocalDateTime requestTime;
        private MicRequestStatus status;
        
        public enum MicRequestStatus {
            PENDING,
            ACCEPTED,
            REJECTED
        }
    }
}