package com.example.danmaku.model;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("danmaku")
public class Danmaku {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField(value = "content", insertStrategy = FieldStrategy.NOT_EMPTY)
    private String content;  // 弹幕内容
    
    @TableField("color")
    private String color;    // 弹幕颜色
    
    @TableField("font_size")
    private Integer fontSize; // 字体大小
    
    @TableField("time")
    private Double time;     // 视频时间点
    
    @TableField("video_id")
    private String videoId;  // 关联的视频ID
    
    @TableField("user_id")
    private String userId;   // 发送用户ID
    
    @TableField("username")
    private String username; // 用户名
    
    @TableField("created_at")
    private LocalDateTime createdAt; // 创建时间
}