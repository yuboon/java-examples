package com.example.livestream.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_room")
public class LiveRoom {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private String title;
    
    private String coverUrl;
    
    private Long userId;
    
    private String streamKey;  // 推流密钥
    
    private String streamUrl;  // 推流地址
    
    private String hlsUrl;     // HLS播放地址
    
    private String flvUrl;     // HTTP-FLV播放地址
    
    private Integer status;    // 0:未开播 1:直播中 2:直播结束
    
    private Long viewCount;    // 观看人数
    
    private Long likeCount;    // 点赞数
    
    private LocalDateTime startTime;  // 开播时间
    
    private LocalDateTime endTime;    // 结束时间
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}