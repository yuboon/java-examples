package com.example.firewall.interceptor;

import com.example.firewall.entity.FirewallRule;
import com.example.firewall.entity.FirewallAccessLog;
import com.example.firewall.service.RuleManager;
import com.example.firewall.service.FirewallService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 防火墙拦截器
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Slf4j
@Component
public class FirewallInterceptor implements HandlerInterceptor {
    
    @Autowired
    private RuleManager ruleManager;
    
    @Autowired
    private FirewallService firewallService;
    
    @Value("${firewall.default.qps-limit:100}")
    private int defaultQpsLimit;
    
    @Value("${firewall.default.user-limit:1000}")
    private int defaultUserLimit;
    
    @Value("${firewall.default.time-window:60}")
    private int defaultTimeWindow;
    
    @Value("${firewall.cache.max-size:10000}")
    private int cacheMaxSize;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * QPS限制缓存 - 存储每个IP+API的访问计数
     */
    private final Cache<String, AtomicInteger> qpsCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.MINUTES)
            .build();
    
    /**
     * 用户限制缓存 - 存储每个IP的访问计数
     */
    private final Cache<String, AtomicInteger> userCache = CacheBuilder.newBuilder()
            .maximumSize(10000)
            .expireAfterWrite(1, TimeUnit.HOURS)
            .build();
    
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        long startTime = System.currentTimeMillis();
        String ipAddress = getClientIpAddress(request);
        String apiPath = request.getRequestURI();
        String userAgent = request.getHeader("User-Agent");
        String method = request.getMethod();
        
        log.debug("防火墙拦截检查: IP={}, API={}, Method={}", ipAddress, apiPath, method);
        
        try {
            // 1. 检查白名单
            if (ruleManager.isWhitelisted(ipAddress)) {
                log.debug("IP {} 在白名单中，允许访问", ipAddress);
                logAccess(ipAddress, apiPath, userAgent, method, 200, null, startTime);
                return true;
            }
            
            // 2. 检查黑名单
            if (ruleManager.isBlacklisted(ipAddress)) {
                log.warn("IP {} 在黑名单中，拒绝访问", ipAddress);
                blockRequest(response, "IP地址被列入黑名单", 403);
                logAccess(ipAddress, apiPath, userAgent, method, 403, "IP黑名单拦截", startTime);
                return false;
            }
            
            // 3. 获取匹配的防火墙规则
            FirewallRule rule = ruleManager.getMatchingRule(apiPath);
            if (rule == null) {
                // 使用默认规则
                rule = createDefaultRule(apiPath);
            }
            
            // 4. QPS限制检查
            if (!checkQpsLimit(ipAddress, apiPath, rule)) {
                log.warn("IP {} 访问 {} 超过QPS限制 {}", ipAddress, apiPath, rule.getEffectiveQpsLimit());
                blockRequest(response, "访问频率过高，请稍后再试", 429);
                logAccess(ipAddress, apiPath, userAgent, method, 429, "QPS限制拦截", startTime);
                return false;
            }
            
            // 5. 用户限制检查
            if (!checkUserLimit(ipAddress, rule)) {
                log.warn("IP {} 超过用户限制 {}", ipAddress, rule.getEffectiveUserLimit());
                blockRequest(response, "访问次数超过限制，请稍后再试", 429);
                logAccess(ipAddress, apiPath, userAgent, method, 429, "用户限制拦截", startTime);
                return false;
            }
            
            // 6. 记录正常访问
            logAccess(ipAddress, apiPath, userAgent, method, 200, null, startTime);
            log.debug("IP {} 访问 {} 通过防火墙检查", ipAddress, apiPath);
            return true;
            
        } catch (Exception e) {
            log.error("防火墙拦截器处理异常: IP={}, API={}", ipAddress, apiPath, e);
            logAccess(ipAddress, apiPath, userAgent, method, 500, "系统异常", startTime);
            return true; // 异常情况下允许通过，避免影响正常业务
        }
    }
    
    /**
     * 检查QPS限制
     * 
     * @param ipAddress IP地址
     * @param apiPath API路径
     * @param rule 防火墙规则
     * @return 是否通过检查
     */
    private boolean checkQpsLimit(String ipAddress, String apiPath, FirewallRule rule) {
        String key = ipAddress + ":" + apiPath;
        int qpsLimit = rule.getEffectiveQpsLimit();
        
        if (qpsLimit <= 0) {
            return true; // 无限制
        }
        
        AtomicInteger counter = qpsCache.getIfPresent(key);
        if (counter == null) {
            counter = new AtomicInteger(0);
            qpsCache.put(key, counter);
        }
        
        int currentCount = counter.incrementAndGet();
        return currentCount <= qpsLimit;
    }
    
    /**
     * 检查用户限制
     * 
     * @param ipAddress IP地址
     * @param rule 防火墙规则
     * @return 是否通过检查
     */
    private boolean checkUserLimit(String ipAddress, FirewallRule rule) {
        int userLimit = rule.getEffectiveUserLimit();
        
        if (userLimit <= 0) {
            return true; // 无限制
        }
        
        AtomicInteger counter = userCache.getIfPresent(ipAddress);
        if (counter == null) {
            counter = new AtomicInteger(0);
            userCache.put(ipAddress, counter);
        }
        
        int currentCount = counter.incrementAndGet();
        return currentCount <= userLimit;
    }
    
    /**
     * 创建默认规则
     * 
     * @param apiPath API路径
     * @return 默认规则
     */
    private FirewallRule createDefaultRule(String apiPath) {
        FirewallRule rule = new FirewallRule();
        rule.setRuleName("默认规则");
        rule.setApiPattern(apiPath);
        rule.setQpsLimit(defaultQpsLimit);
        rule.setUserLimit(defaultUserLimit);
        rule.setTimeWindow(defaultTimeWindow);
        rule.setEnabled(true);
        return rule;
    }
    
    /**
     * 阻止请求并返回错误响应
     * 
     * @param response HTTP响应
     * @param message 错误消息
     * @param statusCode 状态码
     * @throws IOException IO异常
     */
    private void blockRequest(HttpServletResponse response, String message, int statusCode) throws IOException {
        response.setStatus(statusCode);
        response.setContentType("application/json;charset=UTF-8");
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("code", statusCode);
        result.put("message", message);
        result.put("timestamp", LocalDateTime.now().toString());
        
        String jsonResponse = objectMapper.writeValueAsString(result);
        response.getWriter().write(jsonResponse);
        response.getWriter().flush();
    }
    
    /**
     * 记录访问日志
     * 
     * @param ipAddress IP地址
     * @param apiPath API路径
     * @param userAgent User-Agent
     * @param method 请求方法
     * @param statusCode 状态码
     * @param blockReason 拦截原因
     * @param startTime 开始时间
     */
    private void logAccess(String ipAddress, String apiPath, String userAgent, String method, 
                          int statusCode, String blockReason, long startTime) {
        try {
            FirewallAccessLog accessLog = new FirewallAccessLog();
            accessLog.setIpAddress(ipAddress);
            accessLog.setApiPath(apiPath);
            accessLog.setUserAgent(userAgent);
            accessLog.setRequestMethod(method);
            accessLog.setStatusCode(statusCode);
            accessLog.setBlockReason(blockReason);
            accessLog.setRequestTime(LocalDateTime.now());
            accessLog.setResponseTime(System.currentTimeMillis() - startTime);
            
            // 异步记录日志，避免影响性能
            firewallService.logAccessAsync(accessLog);
            
        } catch (Exception e) {
            log.error("记录访问日志失败: IP={}, API={}", ipAddress, apiPath, e);
        }
    }
    
    /**
     * 获取客户端真实IP地址
     * 
     * @param request HTTP请求
     * @return IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String[] headers = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED",
            "HTTP_VIA",
            "REMOTE_ADDR"
        };
        
        for (String header : headers) {
            String ip = request.getHeader(header);
            if (ip != null && ip.length() != 0 && !"unknown".equalsIgnoreCase(ip)) {
                // 多个IP时取第一个
                if (ip.contains(",")) {
                    ip = ip.split(",")[0].trim();
                }
                return ip;
            }
        }
        
        return request.getRemoteAddr();
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("qpsCacheSize", qpsCache.size());
        stats.put("userCacheSize", userCache.size());
        stats.put("qpsCacheStats", qpsCache.stats());
        stats.put("userCacheStats", userCache.stats());
        return stats;
    }
    
    /**
     * 清理缓存
     */
    public void clearCache() {
        qpsCache.invalidateAll();
        userCache.invalidateAll();
        log.info("防火墙拦截器缓存已清理");
    }
}