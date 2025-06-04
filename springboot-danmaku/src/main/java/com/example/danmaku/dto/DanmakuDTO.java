package com.example.danmaku.dto;

import lombok.Data;

@Data
public class DanmakuDTO {
    private String content;
    private String color = "#ffffff"; // 默认白色
    private Integer fontSize = 24;    // 默认字体大小
    private Double time;
    private String videoId;
    private String userId;
    private String username;
}