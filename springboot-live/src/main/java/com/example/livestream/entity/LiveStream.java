package com.example.livestream.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_stream")
public class LiveStream {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long roomId;
    
    private String streamId;  // 流ID
    
    private String protocol;  // 协议类型：rtmp/hls/flv
    
    private Integer bitrate;  // 码率
    
    private String resolution; // 分辨率
    
    private Integer status;   // 0:未启动 1:活跃 2:已结束
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}