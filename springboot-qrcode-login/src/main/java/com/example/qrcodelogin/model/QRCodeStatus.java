package com.example.qrcodelogin.model;

import lombok.Data;

@Data
public class QRCodeStatus {
    public static final String WAITING = "WAITING";   // 等待扫描
    public static final String SCANNED = "SCANNED";   // 已扫描
    public static final String CONFIRMED = "CONFIRMED"; // 已确认
    public static final String CANCELLED = "CANCELLED"; // 已取消
    public static final String EXPIRED = "EXPIRED";   // 已过期
    
    private String qrCodeId;     // 二维码ID
    private String status;       // 状态
    private UserInfo userInfo;   // 用户信息
    private long createTime;     // 创建时间
    
    public QRCodeStatus() {
        this.createTime = System.currentTimeMillis();
    }
    
    public QRCodeStatus(String qrCodeId, String status) {
        this.qrCodeId = qrCodeId;
        this.status = status;
        this.createTime = System.currentTimeMillis();
    }
}