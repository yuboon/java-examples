package com.example.firewall.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;

/**
 * 防火墙黑名单实体类
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirewallBlacklist {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 封禁原因
     */
    private String reason;
    
    /**
     * 过期时间（NULL表示永久）
     */
    private LocalDateTime expireTime;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 检查IP是否已过期
     * 
     * @return 是否已过期
     */
    public boolean isExpired() {
        return expireTime != null && LocalDateTime.now().isAfter(expireTime);
    }
    
    /**
     * 检查IP是否有效（启用且未过期）
     * 
     * @return 是否有效
     */
    public boolean isValid() {
        return (enabled == null || enabled) && !isExpired();
    }
    
    /**
     * 检查IP地址是否匹配
     * 
     * @param ip 要检查的IP地址
     * @return 是否匹配
     */
    public boolean matches(String ip) {
        if (ipAddress == null || ip == null) {
            return false;
        }
        
        // 支持CIDR格式的IP段匹配
        if (ipAddress.contains("/")) {
            return matchesCidr(ip, ipAddress);
        }
        
        // 支持通配符匹配
        if (ipAddress.contains("*")) {
            String pattern = ipAddress.replace("*", ".*");
            return ip.matches(pattern);
        }
        
        // 精确匹配
        return ipAddress.equals(ip);
    }
    
    /**
     * CIDR格式IP段匹配
     * 
     * @param ip IP地址
     * @param cidr CIDR格式的IP段
     * @return 是否匹配
     */
    private boolean matchesCidr(String ip, String cidr) {
        try {
            String[] parts = cidr.split("/");
            if (parts.length != 2) {
                return false;
            }
            
            String networkIp = parts[0];
            int prefixLength = Integer.parseInt(parts[1]);
            
            // 简单的IPv4 CIDR匹配实现
            long ipLong = ipToLong(ip);
            long networkLong = ipToLong(networkIp);
            long mask = (0xFFFFFFFFL << (32 - prefixLength)) & 0xFFFFFFFFL;
            
            return (ipLong & mask) == (networkLong & mask);
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * IP地址转换为长整型
     * 
     * @param ip IP地址
     * @return 长整型值
     */
    private long ipToLong(String ip) {
        String[] parts = ip.split("\\.");
        if (parts.length != 4) {
            throw new IllegalArgumentException("Invalid IP address: " + ip);
        }
        
        long result = 0;
        for (int i = 0; i < 4; i++) {
            result = (result << 8) + Integer.parseInt(parts[i]);
        }
        return result;
    }
}