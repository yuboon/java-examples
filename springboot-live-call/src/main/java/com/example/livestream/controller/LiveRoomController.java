package com.example.livestream.controller;

import com.example.livestream.model.LiveRoom;
import com.example.livestream.service.LiveRoomService;
import com.example.livestream.util.JwtTokenUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/rooms")
@Slf4j
public class LiveRoomController {

    @Autowired
    private LiveRoomService liveRoomService;

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @PostMapping
    public ResponseEntity<?> createRoom(@RequestBody Map<String, String> roomRequest) {
        String title = roomRequest.get("title");
        LiveRoom room = liveRoomService.createLiveRoom(title, jwtTokenUtil.getUsername());
        
        if (room != null) {
            return ResponseEntity.ok(room);
        } else {
            return ResponseEntity.badRequest().body("Failed to create room");
        }
    }

    @PostMapping("/{roomId}/mic-response")
    public ResponseEntity<?> handleMicResponse(
            @PathVariable String roomId,
            @RequestParam String username,
            @RequestParam boolean accept) {

        boolean result = liveRoomService.handleMicRequest(roomId, username, accept, jwtTokenUtil.getUsername());
        if (result) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Failed to handle mic request");
        }
    }

    @GetMapping
    public ResponseEntity<List<LiveRoom>> getAllRooms() {
        return ResponseEntity.ok(liveRoomService.getAllActiveLiveRooms());
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<?> getRoomInfo(@PathVariable String roomId) {
        LiveRoom room = liveRoomService.getLiveRoom(roomId);
        if (room != null) {
            return ResponseEntity.ok(room);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{roomId}/join")
    public ResponseEntity<?> joinRoom(@PathVariable String roomId) {
        boolean joined = liveRoomService.joinLiveRoom(roomId, jwtTokenUtil.getUsername());
        if (joined) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Failed to join room");
        }
    }

    @PostMapping("/{roomId}/leave")
    public ResponseEntity<?> leaveRoom(@PathVariable String roomId) {
        boolean left = liveRoomService.leaveLiveRoom(roomId, jwtTokenUtil.getUsername());
        if (left) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Failed to leave room");
        }
    }

    @PostMapping("/{roomId}/close")
    public ResponseEntity<?> closeRoom(@PathVariable String roomId) {
        boolean closed = liveRoomService.closeLiveRoom(roomId, jwtTokenUtil.getUsername());
        if (closed) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.badRequest().body("Failed to close room");
        }
    }

    @GetMapping("/{roomId}/mic-status")
    public ResponseEntity<?> getMicStatus(
            @PathVariable String roomId,
            @RequestParam String username) {

        if (!jwtTokenUtil.getUsername().equals(username)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Unauthorized");
        }

        LiveRoom room = liveRoomService.getLiveRoom(roomId);
        if (room == null) {
            return ResponseEntity.notFound().build();
        }

        LiveRoom.MicRequest request = room.getMicRequests().get(username);
        Map<String, Object> result = new HashMap<>();

        if (request == null) {
            // 没有找到请求，可能是已经被处理并移除
            if (room.getActiveMicUsers().contains(username)) {
                result.put("status", "ACCEPTED");
            } else {
                result.put("status", "NOT_FOUND");
            }
        } else {
            result.put("status", request.getStatus().toString());
        }

        return ResponseEntity.ok(result);
    }
}