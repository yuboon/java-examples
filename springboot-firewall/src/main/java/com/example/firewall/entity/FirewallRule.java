package com.example.firewall.entity;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 防火墙规则实体类
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FirewallRule {
    
    /**
     * 主键ID
     */
    private Long id;
    
    /**
     * 规则名称
     */
    private String ruleName;
    
    /**
     * API路径匹配模式
     */
    private String apiPattern;
    
    /**
     * QPS限制（每秒最大请求数）
     */
    private Integer qpsLimit;
    
    /**
     * 单用户时间窗限制（分钟内最大请求数）
     */
    private Integer userLimit;
    
    /**
     * 时间窗口（秒）
     */
    private Integer timeWindow;
    
    /**
     * 是否启用
     */
    private Boolean enabled;
    
    /**
     * 规则描述
     */
    private String description;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdTime;
    
    /**
     * 更新时间
     */
    private LocalDateTime updatedTime;
    
    /**
     * 黑名单IP列表（运行时使用，不存储在数据库）
     */
    private List<String> blackIps;
    
    /**
     * 白名单IP列表（运行时使用，不存储在数据库）
     */
    private List<String> whiteIps;
    
    /**
     * 检查API路径是否匹配此规则
     * 
     * @param apiPath API路径
     * @return 是否匹配
     */
    public boolean matches(String apiPath) {
        if (apiPattern == null || apiPath == null) {
            return false;
        }
        
        // 支持通配符匹配
        String pattern = apiPattern.replace("**", ".*").replace("*", "[^/]*");
        return apiPath.matches(pattern);
    }
    
    /**
     * 获取有效的QPS限制
     * 
     * @return QPS限制，默认100
     */
    public int getEffectiveQpsLimit() {
        return qpsLimit != null && qpsLimit > 0 ? qpsLimit : 100;
    }
    
    /**
     * 获取有效的用户限制
     * 
     * @return 用户限制，默认60
     */
    public int getEffectiveUserLimit() {
        return userLimit != null && userLimit > 0 ? userLimit : 60;
    }
    
    /**
     * 获取有效的时间窗口
     * 
     * @return 时间窗口，默认60秒
     */
    public int getEffectiveTimeWindow() {
        return timeWindow != null && timeWindow > 0 ? timeWindow : 60;
    }
    
    /**
     * 检查规则是否启用
     * 
     * @return 是否启用，默认true
     */
    public boolean isEffectiveEnabled() {
        return enabled == null || enabled;
    }
}