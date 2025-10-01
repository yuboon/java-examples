package com.license.controller;

import com.license.entity.License;
import com.license.service.KeyManagementService;
import com.license.service.LicenseService;
import com.license.util.HardwareUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * 许可证控制器
 */
@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class LicenseController {

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private KeyManagementService keyManagementService;

    @Autowired
    private HardwareUtil hardwareUtil;

    /**
     * 生成新的密钥对
     */
    @PostMapping("/keys/generate")
    public ResponseEntity<Map<String, Object>> generateKeys() {
        try {
            Map<String, String> keys = keyManagementService.generateKeyPair();
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", keys);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "密钥生成失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 加载密钥
     */
    @PostMapping("/keys/load")
    public ResponseEntity<Map<String, Object>> loadKeys(@RequestBody Map<String, String> request) {
        try {
            String privateKey = request.get("privateKey");
            String publicKey = request.get("publicKey");

            if (privateKey != null && !privateKey.trim().isEmpty()) {
                keyManagementService.loadPrivateKey(privateKey);
            }

            if (publicKey != null && !publicKey.trim().isEmpty()) {
                keyManagementService.loadPublicKey(publicKey);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "密钥加载成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "密钥加载失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 生成许可证
     */
    @PostMapping("/license/generate")
    public ResponseEntity<Map<String, Object>> generateLicense(@RequestBody License license) {
        try {
            if (!keyManagementService.isKeysLoaded()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "请先加载或生成密钥");
                return ResponseEntity.badRequest().body(response);
            }

            String licenseJson = licenseService.generateLicense(license, keyManagementService.getCachedPrivateKey());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", licenseJson);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "许可证生成失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 验证许可证
     */
    @PostMapping("/license/verify")
    public ResponseEntity<Map<String, Object>> verifyLicense(@RequestBody Map<String, String> request) {
        try {
            String licenseJson = request.get("licenseJson");

            if (!keyManagementService.isKeysLoaded()) {
                Map<String, Object> response = new HashMap<>();
                response.put("success", false);
                response.put("message", "请先加载公钥");
                return ResponseEntity.badRequest().body(response);
            }

            LicenseService.LicenseVerifyResult result = licenseService.verifyLicense(
                licenseJson, keyManagementService.getCachedPublicKey());

            Map<String, Object> response = new HashMap<>();
            response.put("success", result.isValid());
            response.put("message", result.getMessage());
            if (result.getLicense() != null) {
                response.put("license", result.getLicense());
            }
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "许可证验证失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * 获取当前硬件信息
     */
    @GetMapping("/hardware/info")
    public ResponseEntity<Map<String, Object>> getHardwareInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);

        Map<String, String> hardwareInfo = new HashMap<>();
        hardwareInfo.put("motherboardSerial", hardwareUtil.getMotherboardSerial());
        hardwareInfo.put("systemInfo", hardwareUtil.getSystemInfo());

        response.put("data", hardwareInfo);
        return ResponseEntity.ok(response);
    }

    /**
     * 检查密钥状态
     */
    @GetMapping("/keys/status")
    public ResponseEntity<Map<String, Object>> getKeysStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("keysLoaded", keyManagementService.isKeysLoaded());
        return ResponseEntity.ok(response);
    }
}