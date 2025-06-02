package com.example.qrcodelogin.controller;

import com.example.qrcodelogin.model.QRCodeStatus;
import com.example.qrcodelogin.model.UserInfo;
import com.example.qrcodelogin.service.QRCodeService;
import com.example.qrcodelogin.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/qrcode")
public class QRCodeController {
    
    @Autowired
    private QRCodeService qrCodeService;
    
    @Autowired
    private UserService userService;
    
    /**
     * 生成二维码
     */
    @GetMapping("/generate")
    public ResponseEntity<QRCodeStatus> generateQRCode() {
        QRCodeStatus qrCodeStatus = qrCodeService.generateQRCode();
        log.info("Generated QR code: {}", qrCodeStatus.getQrCodeId());
        return ResponseEntity.ok(qrCodeStatus);
    }
    
    /**
     * 获取二维码图片
     */
    @GetMapping(value = "/image/{qrCodeId}", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getQRCodeImage(@PathVariable String qrCodeId, HttpServletRequest request) {
        // 获取基础URL
        String baseUrl = request.getScheme() + "://" + request.getServerName();
        if (request.getServerPort() != 80 && request.getServerPort() != 443) {
            baseUrl += ":" + request.getServerPort();
        }
        
        byte[] qrCodeImage = qrCodeService.generateQRCodeImage(qrCodeId, baseUrl);
        if (qrCodeImage != null) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_PNG)
                    .body(qrCodeImage);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }
    
    /**
     * 扫描二维码
     */
    @PostMapping("/scan")
    public ResponseEntity<String> scanQRCode(@RequestBody Map<String, String> request) {
        String qrCodeId = request.get("qrCodeId");
        if (qrCodeId == null) {
            return ResponseEntity.badRequest().body("QR code ID is required");
        }
        
        boolean updated = qrCodeService.updateQRCodeStatus(qrCodeId, QRCodeStatus.SCANNED);
        if (!updated) {
            return ResponseEntity.badRequest().body("Invalid QR code");
        }
        
        log.info("QR code scanned: {}", qrCodeId);
        return ResponseEntity.ok("Scanned successfully");
    }
    
    /**
     * 确认登录
     */
    @PostMapping("/confirm")
    public ResponseEntity<String> confirmLogin(@RequestBody ConfirmLoginRequest request) {
        if (request.getQrCodeId() == null || request.getUserId() == null) {
            return ResponseEntity.badRequest().body("QR code ID and user ID are required");
        }
        
        // 模拟用户登录
        UserInfo userInfo = userService.login(request.getUserId());
        if (userInfo == null) {
            return ResponseEntity.badRequest().body("User not found");
        }
        
        boolean confirmed = qrCodeService.confirmLogin(request.getQrCodeId(), userInfo);
        if (!confirmed) {
            return ResponseEntity.badRequest().body("Invalid QR code or status");
        }
        
        log.info("Login confirmed: {}, user: {}", request.getQrCodeId(), request.getUserId());
        return ResponseEntity.ok("Login confirmed successfully");
    }
    
    /**
     * 取消登录
     */
    @PostMapping("/cancel")
    public ResponseEntity<String> cancelLogin(@RequestBody Map<String, String> request) {
        String qrCodeId = request.get("qrCodeId");
        if (qrCodeId == null) {
            return ResponseEntity.badRequest().body("QR code ID is required");
        }
        
        boolean cancelled = qrCodeService.cancelLogin(qrCodeId);
        if (!cancelled) {
            return ResponseEntity.badRequest().body("Invalid QR code");
        }
        
        log.info("Login cancelled: {}", qrCodeId);
        return ResponseEntity.ok("Login cancelled successfully");
    }
    
    /**
     * 获取二维码状态
     */
    @GetMapping("/status/{qrCodeId}")
    public ResponseEntity<QRCodeStatus> getQRCodeStatus(@PathVariable String qrCodeId) {
        QRCodeStatus qrCodeStatus = qrCodeService.getQRCodeStatus(qrCodeId);
        if (qrCodeStatus == null) {
            return ResponseEntity.badRequest().body(null);
        }
        
        return ResponseEntity.ok(qrCodeStatus);
    }
    
    @Data
    public static class ConfirmLoginRequest {
        private String qrCodeId;
        private String userId;
    }
}