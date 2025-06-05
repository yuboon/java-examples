package com.example.livestream.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("live_recording")
public class LiveRecording {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long roomId;
    
    private String fileName;
    
    private String fileUrl;
    
    private Long fileSize;
    
    private Integer duration;  // 时长，单位秒
    
    private LocalDateTime startTime;
    
    private LocalDateTime endTime;
    
    private Integer status;   // 0:录制中 1:录制完成 2:处理中 3:可用 4:删除
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}