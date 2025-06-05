package com.example.livestream.controller;

import com.example.livestream.entity.LiveRecording;
import com.example.livestream.entity.LiveRoom;
import com.example.livestream.mapper.LiveRoomMapper;
import com.example.livestream.service.LiveRecordingService;
import com.example.livestream.service.LiveStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/live")
@Slf4j
public class LiveController {
    
    @Autowired
    private LiveStreamService liveStreamService;
    
    @Autowired
    private LiveRecordingService recordingService;

    @Autowired
    private LiveRoomMapper liveRoomMapper;
    
    /**
     * 创建直播间
     */
    @PostMapping("/room")
    public ResponseEntity<LiveRoom> createLiveRoom(@RequestBody LiveRoom liveRoom) {
        try {
            LiveRoom createdRoom = liveStreamService.createLiveRoom(liveRoom);
            return ResponseEntity.ok(createdRoom);
        } catch (Exception e) {
            log.error("创建直播间异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取直播间详情
     */
    @GetMapping("/room/{roomId}")
    public ResponseEntity<LiveRoom> getLiveRoom(@PathVariable Long roomId) {
        try {
            LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
            if (liveRoom == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok(liveRoom);
        } catch (Exception e) {
            log.error("获取直播间详情异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 开始直播
     */
    @PostMapping("/room/{roomId}/start")
    public ResponseEntity<LiveRoom> startLiveStream(@PathVariable Long roomId) {
        try {
            LiveRoom liveRoom = liveStreamService.startLiveStream(roomId);
            return ResponseEntity.ok(liveRoom);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("开始直播异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 结束直播
     */
    @PostMapping("/room/{roomId}/end")
    public ResponseEntity<LiveRoom> endLiveStream(@PathVariable Long roomId) {
        try {
            LiveRoom liveRoom = liveStreamService.endLiveStream(roomId);
            return ResponseEntity.ok(liveRoom);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("结束直播异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取活跃直播间列表
     */
    @GetMapping("/rooms/active")
    public ResponseEntity<List<LiveRoom>> getActiveLiveRooms(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<LiveRoom> rooms = liveStreamService.getActiveLiveRooms(page, size);
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            log.error("获取活跃直播间列表异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取热门直播间
     */
    @GetMapping("/rooms/hot")
    public ResponseEntity<List<LiveRoom>> getHotLiveRooms(
            @RequestParam(defaultValue = "10") int limit) {
        try {
            List<LiveRoom> rooms = liveStreamService.getHotLiveRooms(limit);
            return ResponseEntity.ok(rooms);
        } catch (Exception e) {
            log.error("获取热门直播间异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 增加观看人数
     */
    @PostMapping("/room/{roomId}/view")
    public ResponseEntity<Void> incrementViewCount(@PathVariable Long roomId) {
        try {
            liveStreamService.incrementViewCount(roomId);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("增加观看人数异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 开始录制直播
     */
    @PostMapping("/room/{roomId}/record/start")
    public ResponseEntity<LiveRecording> startRecording(@PathVariable Long roomId) {
        try {
            LiveRecording recording = recordingService.startRecording(roomId);
            return ResponseEntity.ok(recording);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("开始录制直播异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 停止录制直播
     */
    @PostMapping("/record/{recordingId}/stop")
    public ResponseEntity<LiveRecording> stopRecording(@PathVariable Long recordingId) {
        try {
            LiveRecording recording = recordingService.stopRecording(recordingId);
            return ResponseEntity.ok(recording);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("停止录制直播异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    /**
     * 获取直播回放列表
     */
    @GetMapping("/room/{roomId}/recordings")
    public ResponseEntity<List<LiveRecording>> getRecordings(
            @PathVariable Long roomId,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size) {
        try {
            List<LiveRecording> recordings = recordingService.getRecordings(roomId, page, size);
            return ResponseEntity.ok(recordings);
        } catch (Exception e) {
            log.error("获取直播回放列表异常", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}