package com.example.mutualcert.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import com.example.mutualcert.security.CustomX509Validator;

import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final CustomX509Validator customX509Validator;

    public ApiController(CustomX509Validator customX509Validator) {
        this.customX509Validator = customX509Validator;
    }

    @GetMapping("/public/info")
    public ResponseEntity<Map<String, Object>> publicInfo() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "这是公开信息，无需认证即可访问");
        response.put("status", "success");
        response.put("timestamp", System.currentTimeMillis());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/secure/data")
    public ResponseEntity<Map<String, Object>> secureData(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 使用自定义证书验证器验证客户端证书
            Object credentials = authentication.getCredentials();
            if (credentials instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) credentials;

                // 执行自定义证书验证
                CustomX509Validator.ValidationResult validationResult =
                    customX509Validator.validateCertificate(cert);

                // 添加验证结果到响应中
                response.put("certificateValidation", Map.of(
                    "valid", validationResult.isValid(),
                    "hasErrors", validationResult.hasErrors(),
                    "hasWarnings", validationResult.hasWarnings(),
                    "errors", validationResult.getErrors(),
                    "warnings", validationResult.getWarnings(),
                    "info", validationResult.getInfo()
                ));

                // 如果验证失败，返回详细的错误信息
                if (!validationResult.isValid()) {
                    response.put("message", "❌ 证书验证失败，访问被拒绝");
                    response.put("error", "CERTIFICATE_VALIDATION_FAILED");
                    response.put("errorDetails", validationResult.getErrors());
                    response.put("status", "error");
                    return ResponseEntity.status(403).body(response);
                }

                // 验证成功，显示详细信息
                response.put("message", "✅ 证书验证成功！" + authentication.getName() + "，您通过了自定义安全验证");
                response.put("certificateStatus", "VALIDATED");
                response.put("validationTimestamp", System.currentTimeMillis());

            } else {
                response.put("message", "⚠️ 无法获取客户端证书进行验证");
                response.put("certificateStatus", "NOT_AVAILABLE");
            }

        } catch (Exception e) {
            response.put("message", "❌ 证书验证过程中发生错误: " + e.getMessage());
            response.put("error", "VALIDATION_EXCEPTION");
            response.put("status", "error");
            return ResponseEntity.status(500).body(response);
        }

        // 添加基础用户信息
        response.put("username", authentication.getName());
        response.put("authorities", authentication.getAuthorities());
        response.put("status", "success");
        response.put("timestamp", System.currentTimeMillis());
        response.put("accessLevel", "CUSTOM_VALIDATED_SECURE");

        return ResponseEntity.ok(response);
    }

    @GetMapping("/certificate/info")
    public ResponseEntity<Map<String, Object>> getCertificateInfo(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        // 从认证对象中获取证书信息
        Object credentials = authentication.getCredentials();

        if (credentials instanceof X509Certificate) {
            X509Certificate cert = (X509Certificate) credentials;
            response.put("subject", cert.getSubjectX500Principal().getName());
            response.put("issuer", cert.getIssuerX500Principal().getName());
            response.put("serialNumber", cert.getSerialNumber().toString());
            response.put("validFrom", cert.getNotBefore());
            response.put("validTo", cert.getNotAfter());
            response.put("sigAlgName", cert.getSigAlgName());
            response.put("type", cert.getType());
        } else {
            response.put("error", "无法获取客户端证书信息");
        }

        response.put("username", authentication.getName());
        response.put("status", "success");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/user/profile")
    public ResponseEntity<Map<String, Object>> getUserProfile(Authentication authentication) {
        Map<String, Object> profile = new HashMap<>();
        profile.put("username", authentication.getName());
        profile.put("authorities", authentication.getAuthorities());

        // 根据不同的用户类型返回不同的配置文件
        if ("DemoClient".equals(authentication.getName())) {
            profile.put("role", "API_CLIENT");
            profile.put("permissions", new String[]{"READ_SENSITIVE_DATA", "WRITE_DATA"});
        } else if ("localhost".equals(authentication.getName())) {
            profile.put("role", "SERVER");
            profile.put("permissions", new String[]{"ADMIN_ACCESS", "SYSTEM_CONFIG"});
        }

        return ResponseEntity.ok(profile);
    }

    @PostMapping("/secure/submit")
    public ResponseEntity<Map<String, Object>> submitData(
            @RequestBody Map<String, Object> requestData,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();
        response.put("message", "数据提交成功");
        response.put("submittedBy", authentication.getName());
        response.put("receivedData", requestData);
        response.put("timestamp", System.currentTimeMillis());
        response.put("status", "success");

        return ResponseEntity.ok(response);
    }

    /**
     * 获取详细的证书验证结果 (包含自定义验证)
     */
    @GetMapping("/certificate/validation")
    public ResponseEntity<Map<String, Object>> getCertificateValidation(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        try {
            // 获取客户端证书
            Object credentials = authentication.getCredentials();

            if (credentials instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) credentials;

                // 执行自定义证书验证
                CustomX509Validator.ValidationResult validationResult =
                    customX509Validator.validateCertificate(cert);

                // 基本证书信息
                response.put("subject", cert.getSubjectX500Principal().getName());
                response.put("issuer", cert.getIssuerX500Principal().getName());
                response.put("serialNumber", cert.getSerialNumber().toString());
                response.put("validFrom", cert.getNotBefore());
                response.put("validTo", cert.getNotAfter());
                response.put("signatureAlgorithm", cert.getSigAlgName());

                // 自定义验证结果
                response.put("customValidation", Map.of(
                    "valid", validationResult.isValid(),
                    "hasErrors", validationResult.hasErrors(),
                    "hasWarnings", validationResult.hasWarnings(),
                    "errors", validationResult.getErrors(),
                    "warnings", validationResult.getWarnings(),
                    "info", validationResult.getInfo()
                ));

                // 提取证书的详细信息
                String subject = cert.getSubjectX500Principal().getName();
                response.put("extractedFields", Map.of(
                    "commonName", extractCN(subject),
                    "organization", extractOrganization(subject),
                    "organizationalUnit", extractOU(subject),
                    "country", extractCountry(subject)
                ));

                response.put("username", authentication.getName());
                response.put("status", "success");

            } else {
                response.put("error", "无法获取客户端证书信息");
                response.put("status", "error");
            }

        } catch (Exception e) {
            response.put("error", "证书验证过程中发生错误: " + e.getMessage());
            response.put("status", "error");
        }

        return ResponseEntity.ok(response);
    }

    /**
     * 验证任意客户端证书 (不需要认证，但需要证书)
     */
    @PostMapping("/certificate/validate")
    public ResponseEntity<Map<String, Object>> validateCertificate(
            @RequestBody Map<String, String> certificateData,
            Authentication authentication) {

        Map<String, Object> response = new HashMap<>();

        try {
            // 获取客户端证书
            if (authentication.getCredentials() instanceof X509Certificate) {
                X509Certificate cert = (X509Certificate) authentication.getCredentials();

                // 执行详细的证书验证
                CustomX509Validator.ValidationResult validationResult =
                    customX509Validator.validateCertificate(cert);

                // 验证结果摘要
                response.put("validationSummary", validationResult.isValid() ? "PASSED" : "FAILED");
                response.put("certificateDetails", Map.of(
                    "subject", cert.getSubjectX500Principal().getName(),
                    "issuer", cert.getIssuerX500Principal().getName(),
                    "serialNumber", cert.getSerialNumber().toString(),
                    "validFrom", cert.getNotBefore(),
                    "validTo", cert.getNotAfter()
                ));

                // 验证详情
                response.put("validationDetails", validationResult);
                response.put("timestamp", System.currentTimeMillis());
                response.put("status", "success");

            } else {
                response.put("error", "请求中没有有效的客户端证书");
                response.put("status", "error");
            }

        } catch (Exception e) {
            response.put("error", "证书验证失败: " + e.getMessage());
            response.put("status", "error");
        }

        return ResponseEntity.ok(response);
    }

    // 辅助方法：提取CN字段
    private String extractCN(String subject) {
        String[] parts = subject.split(",");
        for (String part : parts) {
            if (part.trim().startsWith("CN=")) {
                return part.trim().substring(3);
            }
        }
        return "Unknown";
    }

    // 辅助方法：提取组织信息
    private String extractOrganization(String subject) {
        String[] parts = subject.split(",");
        for (String part : parts) {
            if (part.trim().startsWith("O=")) {
                return part.trim().substring(2);
            }
        }
        return "Unknown";
    }

    // 辅助方法：提取组织部门
    private String extractOU(String subject) {
        String[] parts = subject.split(",");
        for (String part : parts) {
            if (part.trim().startsWith("OU=")) {
                return part.trim().substring(3);
            }
        }
        return "Unknown";
    }

    // 辅助方法：提取国家信息
    private String extractCountry(String subject) {
        String[] parts = subject.split(",");
        for (String part : parts) {
            if (part.trim().startsWith("C=")) {
                return part.trim().substring(2);
            }
        }
        return "Unknown";
    }
}