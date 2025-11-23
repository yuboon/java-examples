package com.example.sign.interceptor;

import com.alibaba.fastjson.JSON;
import com.example.sign.config.ApiSecurityProperties;
import com.example.sign.util.SignatureUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * API 签名验证拦截器
 *
 */
@Slf4j
@Component
public class SignatureValidationInterceptor implements HandlerInterceptor {

    private final ApiSecurityProperties securityProperties;

    public SignatureValidationInterceptor(ApiSecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 如果未启用签名验证，直接放行
        if (!securityProperties.isEnabled()) {
            return true;
        }

        // 记录请求信息
        if (securityProperties.isEnableRequestLog()) {
            log.info("=== API Request ===");
            log.info("URI: {}", request.getRequestURI());
            log.info("Method: {}", request.getMethod());
            log.info("Remote IP: {}", getClientIpAddress(request));
        }

        // 获取请求头中的签名信息
        String timestamp = request.getHeader("X-Timestamp");
        String signature = request.getHeader("X-Signature");
        String apiKey = request.getHeader("X-Api-Key");

        if (securityProperties.isEnableRequestLog()) {
            log.info("Timestamp: {}, API Key: {}", timestamp, apiKey);
        }

        // 验证必要参数
        if (!StringUtils.hasText(timestamp) || !StringUtils.hasText(signature) || !StringUtils.hasText(apiKey)) {
            log.warn("Missing required signature headers. Timestamp: {}, Signature: {}, API Key: {}",
                    StringUtils.hasText(timestamp) ? timestamp : "null",
                    StringUtils.hasText(signature) ? signature.substring(0, Math.min(signature.length(), 10)) + "..." : "null",
                    StringUtils.hasText(apiKey) ? apiKey : "null");

            return writeErrorResponse(response, 401, "Missing required signature headers");
        }

        // 验证时间戳（防重放攻击）
        if (!SignatureUtil.validateTimestamp(timestamp, securityProperties.getTimeTolerance())) {
            log.warn("Invalid timestamp: {}", timestamp);
            return writeErrorResponse(response, 401, "Invalid timestamp");
        }

        // 获取密钥
        String secret = securityProperties.getApiSecret(apiKey);
        if (secret == null) {
            log.warn("Invalid API key: {}", apiKey);
            return writeErrorResponse(response, 401, "Invalid API key");
        }

        // 提取请求参数
        Map<String, Object> params = SignatureUtil.extractParams(request);

        // 验证签名
        if (!SignatureUtil.verifySignature(params, timestamp, secret, signature)) {
            // 生成预期签名用于日志对比（注意：生产环境中不要输出完整签名）
            String expectedSignature = SignatureUtil.generateSignature(params, timestamp, secret);
            log.warn("Signature validation failed. Expected: {}..., Actual: {}...",
                    expectedSignature.substring(0, Math.min(expectedSignature.length(), 10)),
                    signature.substring(0, Math.min(signature.length(), 10)));

            return writeErrorResponse(response, 401, "Invalid signature");
        }

        log.info("Signature validation successful");
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                               Object handler, Exception ex) throws Exception {
        // 记录响应信息
        if (securityProperties.isEnableResponseLog()) {
            log.info("=== API Response ===");
            log.info("Status: {}", response.getStatus());
            log.info("Content-Type: {}", response.getContentType());
        }
    }

    /**
     * 写入错误响应
     *
     * @param response HTTP响应
     * @param status 状态码
     * @param message 错误消息
     * @return 始终返回false
     */
    private boolean writeErrorResponse(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");

        Map<String, Object> errorResult = new HashMap<>();
        errorResult.put("code", status);
        errorResult.put("message", message);
        errorResult.put("timestamp", System.currentTimeMillis());

        String jsonResult = JSON.toJSONString(errorResult);
        response.getWriter().write(jsonResult);
        response.getWriter().flush();

        return false;
    }

    /**
     * 获取客户端真实IP地址
     *
     * @param request HTTP请求
     * @return 客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(xForwardedFor) && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (StringUtils.hasText(xRealIp) && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }
}