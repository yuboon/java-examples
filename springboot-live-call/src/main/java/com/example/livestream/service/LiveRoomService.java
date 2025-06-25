package com.example.livestream.service;

import com.example.livestream.model.LiveRoom;
import com.example.livestream.model.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@Slf4j
public class LiveRoomService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private UserService userService;

    // 内存中存储直播间信息
    private final Map<String, LiveRoom> liveRooms = new ConcurrentHashMap<>();

    public LiveRoom createLiveRoom(String title, String username) {
        User user = userService.getUserByUsername(username);
        if (user == null || user.getRole() != User.UserRole.BROADCASTER) {
            return null;
        }

        String roomId = UUID.randomUUID().toString().substring(0, 8);
        LiveRoom liveRoom = new LiveRoom();
        liveRoom.setRoomId(roomId);
        liveRoom.setTitle(title);
        liveRoom.setBroadcaster(username);
        liveRoom.setCreatedTime(LocalDateTime.now());
        liveRoom.setActive(true);

        liveRooms.put(roomId, liveRoom);
        log.info("Live room created: {} by {}", roomId, username);
        return liveRoom;
    }

    public LiveRoom getLiveRoom(String roomId) {
        return liveRooms.get(roomId);
    }

    public List<LiveRoom> getAllActiveLiveRooms() {
        List<LiveRoom> activeRooms = new ArrayList<>();
        for (LiveRoom room : liveRooms.values()) {
            if (room.isActive()) {
                activeRooms.add(room);
            }
        }
        return activeRooms;
    }

    public boolean joinLiveRoom(String roomId, String username) {
        LiveRoom room = liveRooms.get(roomId);
        if (room == null || !room.isActive()) {
            return false;
        }

        room.getAudiences().add(username);
        log.info("User {} joined room {}", username, roomId);
        return true;
    }

    public boolean leaveLiveRoom(String roomId, String username) {
        LiveRoom room = liveRooms.get(roomId);
        if (room == null) {
            return false;
        }

        room.getAudiences().remove(username);
        room.getActiveMicUsers().remove(username);
        room.getMicRequests().remove(username);
        log.info("User {} left room {}", username, roomId);
        return true;
    }

    public boolean closeLiveRoom(String roomId, String username) {
        LiveRoom room = liveRooms.get(roomId);
        if (room == null || !room.getBroadcaster().equals(username)) {
            return false;
        }

        room.setActive(false);
        log.info("Room {} closed by {}", roomId, username);
        return true;
    }

    public boolean requestMic(String roomId, String username) {
        LiveRoom room = liveRooms.get(roomId);
        if (room == null || !room.isActive()) {
            log.error("Cannot request mic: room {} is null or inactive", roomId);
            return false;
        }

        if (!room.getAudiences().contains(username)) {
            log.info("Adding user {} to room {} audience list", username, roomId);
            room.getAudiences().add(username);
        }

        String broadcaster = room.getBroadcaster();
        if (broadcaster == null || broadcaster.isEmpty()) {
            log.error("Room {} has no broadcaster", roomId);
            return false;
        }

        log.info("Processing mic request from {} to broadcaster {} in room {}",
                username, broadcaster, roomId);

        LiveRoom.MicRequest request = new LiveRoom.MicRequest();
        request.setUsername(username);
        request.setRequestTime(LocalDateTime.now());
        request.setStatus(LiveRoom.MicRequest.MicRequestStatus.PENDING);

        room.getMicRequests().put(username, request);

        // 直接发送给主播
        try {
            // 方法1: 使用转换后的目标路径
            messagingTemplate.convertAndSendToUser(
                    broadcaster,
                    "/queue/mic-requests",
                    request);

            // 方法2: 也尝试使用完整路径发送(作为备份)
            messagingTemplate.convertAndSend(
                    "/user/" + broadcaster + "/queue/mic-requests",
                    request);

            log.info("Mic request sent to broadcaster {} via both methods", broadcaster);
        } catch (Exception e) {
            log.error("Failed to send mic request: {}", e.getMessage(), e);
            return false;
        }

        return true;
    }

    public boolean handleMicRequest(String roomId, String requestUsername,
                                    boolean accept, String broadcasterUsername) {
        LiveRoom room = liveRooms.get(roomId);
        if (room == null || !room.isActive() ||
                !room.getBroadcaster().equals(broadcasterUsername)) {
            log.error("Cannot handle mic request: invalid room or broadcaster");
            return false;
        }

        LiveRoom.MicRequest request = room.getMicRequests().get(requestUsername);
        if (request == null) {
            log.error("Mic request from {} not found", requestUsername);
            return false;
        }

        log.info("Handling mic request from {} in room {}: {}",
                requestUsername, roomId, accept ? "ACCEPTED" : "REJECTED");

        if (accept) {
            request.setStatus(LiveRoom.MicRequest.MicRequestStatus.ACCEPTED);
            room.getActiveMicUsers().add(requestUsername);
        } else {
            request.setStatus(LiveRoom.MicRequest.MicRequestStatus.REJECTED);
        }

        // 通知申请者结果
        try {
            log.info("Sending mic response to user {}: {}",
                    requestUsername, accept ? "ACCEPTED" : "REJECTED");

            // 清晰标记消息类型和状态
            Map<String, Object> response = new HashMap<>();
            response.put("username", requestUsername);
            response.put("status", request.getStatus().toString());
            response.put("roomId", roomId);
            response.put("timestamp", System.currentTimeMillis());

            // 发送消息给观众
            messagingTemplate.convertAndSendToUser(
                    requestUsername,
                    "/queue/mic-response",
                    response);

            log.info("Mic response sent successfully");
        } catch (Exception e) {
            // 下面这行代码是不是会吞掉异常
            log.error("Failed to send mic response: {}", e.getMessage(), e);
            return false;
        }

        return true;
    }

    public boolean endMic(String roomId, String username, String initiator) {
        LiveRoom room = liveRooms.get(roomId);
        if (room == null || !room.isActive()) {
            return false;
        }

        // 只有主播或用户自己可以结束连麦
        if (!room.getBroadcaster().equals(initiator) && !username.equals(initiator)) {
            return false;
        }

        room.getActiveMicUsers().remove(username);
        room.getMicRequests().remove(username);

        // 通知相关用户连麦已结束
        messagingTemplate.convertAndSendToUser(
                username,
                "/queue/mic-ended",
                roomId);

        if (!username.equals(room.getBroadcaster())) {
            messagingTemplate.convertAndSendToUser(
                    room.getBroadcaster(),
                    "/queue/mic-ended",
                    username);
        }

        log.info("Mic ended for {} in room {}, initiated by {}", 
                username, roomId, initiator);
        return true;
    }
}