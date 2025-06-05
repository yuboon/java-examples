package com.example.livestream.controller;

import cn.hutool.core.util.URLUtil;
import cn.hutool.extra.servlet.ServletUtil;
import cn.hutool.http.HttpUtil;
import com.example.livestream.dto.SrsCallbackDto;
import com.example.livestream.service.LiveStreamService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriUtils;
import org.springframework.web.util.WebUtils;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/srs/callback")
@Slf4j
public class SrsCallbackController {
    
    @Autowired
    private LiveStreamService liveStreamService;
    
    /**
     * 处理SRS on_publish回调
     * 当推流开始时，SRS会调用此接口
     */
    @PostMapping("/on_publish")
    public ResponseEntity<Map<String, Object>> onPublish(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_publish回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());
        
        Map<String, Object> result = new HashMap<>();

        String param = callbackDto.getParam();
        Map<String, String> paramMap = HttpUtil.decodeParamMap(param, StandardCharsets.UTF_8);
        String token = paramMap.get("auth_key");
        String expire = paramMap.get("expire");
        callbackDto.setToken(token);
        callbackDto.setExpire(expire);

        // 验证推流密钥
        boolean valid = liveStreamService.validateStreamKey(
                callbackDto.getStream(), 
                callbackDto.getToken(), 
                callbackDto.getExpire());
                
        if (!valid) {
            result.put("code", 403);
            result.put("message", "Forbidden");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
        }
        
        // 处理流发布事件
        liveStreamService.handleStreamPublish(callbackDto.getApp(), callbackDto.getStream());
        
        result.put("code", 0);
        result.put("message", "Success");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 处理SRS on_unpublish回调
     * 当推流结束时，SRS会调用此接口
     */
    @PostMapping("/on_unpublish")
    public ResponseEntity<Map<String, Object>> onUnpublish(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_unpublish回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());
        
        // 处理流关闭事件
        liveStreamService.handleStreamClose(callbackDto.getApp(), callbackDto.getStream());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "Success");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 处理SRS on_play回调
     * 当播放流开始时，SRS会调用此接口
     */
    @PostMapping("/on_play")
    public ResponseEntity<Map<String, Object>> onPlay(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_play回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "Success");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 处理SRS on_stop回调
     * 当播放流结束时，SRS会调用此接口
     */
    @PostMapping("/on_stop")
    public ResponseEntity<Map<String, Object>> onStop(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_stop回调: app={}, stream={}", callbackDto.getApp(), callbackDto.getStream());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "Success");
        return ResponseEntity.ok(result);
    }
    
    /**
     * 处理SRS on_dvr回调
     * 当DVR录制文件关闭时，SRS会调用此接口
     */
    @PostMapping("/on_dvr")
    public ResponseEntity<Map<String, Object>> onDvr(@RequestBody SrsCallbackDto callbackDto) {
        log.info("SRS on_dvr回调: app={}, stream={}, file={}", 
                callbackDto.getApp(), callbackDto.getStream(), callbackDto.getFile());
        
        Map<String, Object> result = new HashMap<>();
        result.put("code", 0);
        result.put("message", "Success");
        return ResponseEntity.ok(result);
    }
}