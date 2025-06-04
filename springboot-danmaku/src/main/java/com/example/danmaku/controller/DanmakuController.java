package com.example.danmaku.controller;

import com.example.danmaku.dto.DanmakuDTO;
import com.example.danmaku.model.Danmaku;
import com.example.danmaku.service.DanmakuService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/danmaku")
public class DanmakuController {
    
    private final DanmakuService danmakuService;
    
    @Autowired
    public DanmakuController(DanmakuService danmakuService) {
        this.danmakuService = danmakuService;
    }
    
    /**
     * 发送弹幕
     */
    @MessageMapping("/danmaku/send")
    public Danmaku sendDanmaku(DanmakuDTO danmakuDTO) {
        return danmakuService.saveDanmaku(danmakuDTO);
    }
    
    /**
     * 获取视频的所有弹幕（REST API）
     */
    @GetMapping("/video/{videoId}")
    public ResponseEntity<List<Danmaku>> getDanmakusByVideoId(@PathVariable String videoId) {
        List<Danmaku> danmakus = danmakuService.getDanmakusByVideoId(videoId);
        return ResponseEntity.ok(danmakus);
    }
    
    /**
     * 获取指定时间范围内的弹幕（REST API）
     */
    @GetMapping("/video/{videoId}/timerange")
    public ResponseEntity<List<Danmaku>> getDanmakusByTimeRange(
            @PathVariable String videoId,
            @RequestParam Double start,
            @RequestParam Double end) {
        List<Danmaku> danmakus = danmakuService.getDanmakusByVideoIdAndTimeRange(videoId, start, end);
        return ResponseEntity.ok(danmakus);
    }
}