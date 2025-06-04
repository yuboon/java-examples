package com.example.danmaku.service;

import com.example.danmaku.dto.DanmakuDTO;
import com.example.danmaku.mapper.DanmakuMapper;
import com.example.danmaku.model.Danmaku;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class DanmakuService {
    
    private final DanmakuMapper danmakuMapper;
    private final SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    public DanmakuService(DanmakuMapper danmakuMapper, SimpMessagingTemplate messagingTemplate) {
        this.danmakuMapper = danmakuMapper;
        this.messagingTemplate = messagingTemplate;
    }
    
    /**
     * 保存并发送弹幕
     */
    public Danmaku saveDanmaku(DanmakuDTO danmakuDTO) {
        // 内容过滤（简单示例）
        String filteredContent = filterContent(danmakuDTO.getContent());
        
        // 创建弹幕实体
        Danmaku danmaku = new Danmaku();
        danmaku.setContent(filteredContent);
        danmaku.setColor(danmakuDTO.getColor());
        danmaku.setFontSize(danmakuDTO.getFontSize());
        danmaku.setTime(danmakuDTO.getTime());
        danmaku.setVideoId(danmakuDTO.getVideoId());
        danmaku.setUserId(danmakuDTO.getUserId());
        danmaku.setUsername(danmakuDTO.getUsername());
        danmaku.setCreatedAt(LocalDateTime.now());
        
        // 保存到数据库
        danmakuMapper.insert(danmaku);
        
        // 通过WebSocket发送到客户端
        messagingTemplate.convertAndSend("/topic/video/" + danmaku.getVideoId(), danmaku);
        
        return danmaku;
    }
    
    /**
     * 获取视频的所有弹幕
     */
    public List<Danmaku> getDanmakusByVideoId(String videoId) {
        return danmakuMapper.findByVideoIdOrderByTimeAsc(videoId);
    }
    
    /**
     * 获取指定时间范围内的弹幕
     */
    public List<Danmaku> getDanmakusByVideoIdAndTimeRange(
            String videoId, Double startTime, Double endTime) {
        return danmakuMapper.findByVideoIdAndTimeBetween(videoId, startTime, endTime);
    }
    
    /**
     * 简单的内容过滤实现
     */
    private String filterContent(String content) {
        // 实际应用中这里可能会有更复杂的过滤逻辑
        String[] sensitiveWords = {"敏感词1", "敏感词2", "敏感词3"};
        String filtered = content;
        
        for (String word : sensitiveWords) {
            filtered = filtered.replaceAll(word, "***");
        }
        
        return filtered;
    }
}