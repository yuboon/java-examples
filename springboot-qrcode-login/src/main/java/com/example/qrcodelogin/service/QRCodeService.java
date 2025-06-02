package com.example.qrcodelogin.service;

import com.example.qrcodelogin.model.QRCodeStatus;
import com.example.qrcodelogin.util.QRCodeUtil;
import com.example.qrcodelogin.ws.QrCodeWebSocketHandler;
import com.example.qrcodelogin.model.UserInfo;
import com.google.zxing.WriterException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class QRCodeService {
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private QrCodeWebSocketHandler webSocketHandler;
    
    @Value("${qrcode.expire.seconds}")
    private long qrCodeExpireSeconds;
    
    @Value("${qrcode.width}")
    private int qrCodeWidth;
    
    @Value("${qrcode.height}")
    private int qrCodeHeight;
    
    private static final String QR_CODE_PREFIX = "qrcode:";
    
    /**
     * 生成二维码
     */
    public QRCodeStatus generateQRCode() {
        String qrCodeId = UUID.randomUUID().toString();
        QRCodeStatus qrCodeStatus = new QRCodeStatus(qrCodeId, QRCodeStatus.WAITING);
        
        // 存储到Redis并设置过期时间
        redisTemplate.opsForValue().set(QR_CODE_PREFIX + qrCodeId, qrCodeStatus, qrCodeExpireSeconds, TimeUnit.SECONDS);
        
        return qrCodeStatus;
    }
    
    /**
     * 生成二维码图片
     */
    public byte[] generateQRCodeImage(String qrCodeId, String baseUrl) {
        try {
            // 构建二维码内容
            String qrCodeContent = baseUrl + "/mobile.html?qrCodeId=" + qrCodeId;
            
            // 生成二维码图片
            return QRCodeUtil.generateQRCodeImage(qrCodeContent, qrCodeWidth, qrCodeHeight);
        } catch (WriterException | IOException e) {
            log.error("Failed to generate QR code image", e);
            return null;
        }
    }
    
    /**
     * 获取二维码状态
     */
    public QRCodeStatus getQRCodeStatus(String qrCodeId) {
        Object obj = redisTemplate.opsForValue().get(QR_CODE_PREFIX + qrCodeId);
        if (obj instanceof QRCodeStatus) {
            return (QRCodeStatus) obj;
        }
        return null;
    }
    
    /**
     * 更新二维码状态
     */
    public boolean updateQRCodeStatus(String qrCodeId, String status) {
        QRCodeStatus qrCodeStatus = getQRCodeStatus(qrCodeId);
        if (qrCodeStatus == null) {
            return false;
        }
        
        qrCodeStatus.setStatus(status);
        redisTemplate.opsForValue().set(QR_CODE_PREFIX + qrCodeId, qrCodeStatus, qrCodeExpireSeconds, TimeUnit.SECONDS);
        
        // 通过WebSocket发送状态更新
        Map<String, Object> message = new HashMap<>();
        message.put("type", "STATUS_CHANGE");
        message.put("status", status);
        webSocketHandler.sendMessage(qrCodeId, message);
        
        return true;
    }
    
    /**
     * 确认登录
     */
    public boolean confirmLogin(String qrCodeId, UserInfo userInfo) {
        QRCodeStatus qrCodeStatus = getQRCodeStatus(qrCodeId);
        if (qrCodeStatus == null || !QRCodeStatus.SCANNED.equals(qrCodeStatus.getStatus())) {
            return false;
        }
        
        qrCodeStatus.setStatus(QRCodeStatus.CONFIRMED);
        qrCodeStatus.setUserInfo(userInfo);
        redisTemplate.opsForValue().set(QR_CODE_PREFIX + qrCodeId, qrCodeStatus, qrCodeExpireSeconds, TimeUnit.SECONDS);
        
        // 通过WebSocket发送状态更新
        Map<String, Object> message = new HashMap<>();
        message.put("type", "STATUS_CHANGE");
        message.put("status", QRCodeStatus.CONFIRMED);
        message.put("userInfo", userInfo);
        webSocketHandler.sendMessage(qrCodeId, message);
        
        return true;
    }
    
    /**
     * 取消登录
     */
    public boolean cancelLogin(String qrCodeId) {
        QRCodeStatus qrCodeStatus = getQRCodeStatus(qrCodeId);
        if (qrCodeStatus == null) {
            return false;
        }
        
        qrCodeStatus.setStatus(QRCodeStatus.CANCELLED);
        redisTemplate.opsForValue().set(QR_CODE_PREFIX + qrCodeId, qrCodeStatus, qrCodeExpireSeconds, TimeUnit.SECONDS);
        
        // 通过WebSocket发送状态更新
        Map<String, Object> message = new HashMap<>();
        message.put("type", "STATUS_CHANGE");
        message.put("status", QRCodeStatus.CANCELLED);
        webSocketHandler.sendMessage(qrCodeId, message);
        
        return true;
    }
    
    /**
     * 定时检查并清理过期的二维码
     */
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void cleanExpiredQRCodes() {
        long currentTime = System.currentTimeMillis();
        long expireTime = currentTime - qrCodeExpireSeconds * 1000;
        
        // 查找所有二维码记录
        Set<String> keys = redisTemplate.keys(QR_CODE_PREFIX + "*");
        if (keys == null || keys.isEmpty()) {
            return;
        }
        
        for (String key : keys) {
            Object obj = redisTemplate.opsForValue().get(key);
            if (obj instanceof QRCodeStatus) {
                QRCodeStatus status = (QRCodeStatus) obj;
                
                // 检查创建时间是否超过过期时间
                if (status.getCreateTime() < expireTime && !QRCodeStatus.EXPIRED.equals(status.getStatus())) {
                    status.setStatus(QRCodeStatus.EXPIRED);
                    redisTemplate.opsForValue().set(key, status, 60, TimeUnit.SECONDS); // 设置一个短的过期时间，让客户端有机会收到过期通知
                    
                    // 发送过期通知
                    Map<String, Object> message = new HashMap<>();
                    message.put("type", "STATUS_CHANGE");
                    message.put("status", QRCodeStatus.EXPIRED);
                    webSocketHandler.sendMessage(status.getQrCodeId(), message);
                    
                    log.info("QR code expired: {}", status.getQrCodeId());
                }
            }
        }
    }
}