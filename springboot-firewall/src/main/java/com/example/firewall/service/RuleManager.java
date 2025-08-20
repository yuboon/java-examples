package com.example.firewall.service;

import com.example.firewall.entity.FirewallRule;
import com.example.firewall.entity.FirewallBlacklist;
import com.example.firewall.entity.FirewallWhitelist;
import com.example.firewall.mapper.FirewallRuleMapper;
import com.example.firewall.mapper.FirewallBlacklistMapper;
import com.example.firewall.mapper.FirewallWhitelistMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * 防火墙规则管理器
 * 
 * @author Firewall Team
 * @version 1.0.0
 */
@Slf4j
@Service
public class RuleManager {
    
    @Autowired
    private FirewallRuleMapper ruleMapper;
    
    @Autowired
    private FirewallBlacklistMapper blacklistMapper;
    
    @Autowired
    private FirewallWhitelistMapper whitelistMapper;
    
    /**
     * 规则缓存
     */
    private final Map<String, FirewallRule> ruleCache = new ConcurrentHashMap<>();
    
    /**
     * 黑名单缓存
     */
    private final Map<String, FirewallBlacklist> blacklistCache = new ConcurrentHashMap<>();
    
    /**
     * 白名单缓存
     */
    private final Map<String, FirewallWhitelist> whitelistCache = new ConcurrentHashMap<>();
    
    /**
     * 初始化加载规则
     */
    @PostConstruct
    public void init() {
        log.info("初始化防火墙规则管理器...");
        refreshRules();
        refreshBlacklist();
        refreshWhitelist();
        log.info("防火墙规则管理器初始化完成，加载规则: {}, 黑名单: {}, 白名单: {}", 
                ruleCache.size(), blacklistCache.size(), whitelistCache.size());
    }
    
    /**
     * 获取匹配指定API路径的规则
     * 
     * @param apiPath API路径
     * @return 匹配的规则，如果没有匹配则返回null
     */
    public FirewallRule getMatchingRule(String apiPath) {
        if (apiPath == null) {
            return null;
        }
        
        // 优先精确匹配
        FirewallRule exactMatch = ruleCache.get(apiPath);
        if (exactMatch != null && exactMatch.isEffectiveEnabled()) {
            return exactMatch;
        }
        
        // 模式匹配
        for (FirewallRule rule : ruleCache.values()) {
            if (rule.isEffectiveEnabled() && rule.matches(apiPath)) {
                return rule;
            }
        }
        
        return null;
    }
    
    /**
     * 检查IP是否在黑名单中
     * 
     * @param ipAddress IP地址
     * @return 是否在黑名单中
     */
    public boolean isBlacklisted(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        
        // 精确匹配
        FirewallBlacklist exactMatch = blacklistCache.get(ipAddress);
        if (exactMatch != null && exactMatch.isValid()) {
            return true;
        }
        
        // 模式匹配
        for (FirewallBlacklist blacklist : blacklistCache.values()) {
            if (blacklist.isValid() && blacklist.matches(ipAddress)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 检查IP是否在白名单中
     * 
     * @param ipAddress IP地址
     * @return 是否在白名单中
     */
    public boolean isWhitelisted(String ipAddress) {
        if (ipAddress == null) {
            return false;
        }
        
        // 精确匹配
        FirewallWhitelist exactMatch = whitelistCache.get(ipAddress);
        if (exactMatch != null && exactMatch.isValid()) {
            return true;
        }
        
        // 模式匹配
        for (FirewallWhitelist whitelist : whitelistCache.values()) {
            if (whitelist.isValid() && whitelist.matches(ipAddress)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * 获取所有规则
     * 
     * @return 规则列表
     */
    public List<FirewallRule> getAllRules() {
        return ruleMapper.findAll();
    }
    
    /**
     * 获取所有黑名单
     * 
     * @return 黑名单列表
     */
    public List<FirewallBlacklist> getAllBlacklist() {
        return blacklistMapper.findAll();
    }
    
    /**
     * 获取所有白名单
     * 
     * @return 白名单列表
     */
    public List<FirewallWhitelist> getAllWhitelist() {
        return whitelistMapper.findAll();
    }
    
    /**
     * 添加或更新规则
     * 
     * @param rule 规则
     * @return 是否成功
     */
    public boolean saveRule(FirewallRule rule) {
        try {
            if (rule.getId() == null) {
                ruleMapper.insert(rule);
            } else {
                ruleMapper.update(rule);
            }
            refreshRules();
            log.info("规则保存成功: {}", rule.getRuleName());
            return true;
        } catch (Exception e) {
            log.error("规则保存失败: {}", rule.getRuleName(), e);
            return false;
        }
    }
    
    /**
     * 删除规则
     * 
     * @param id 规则ID
     * @return 是否成功
     */
    public boolean deleteRule(Long id) {
        try {
            ruleMapper.deleteById(id);
            refreshRules();
            log.info("规则删除成功: {}", id);
            return true;
        } catch (Exception e) {
            log.error("规则删除失败: {}", id, e);
            return false;
        }
    }
    
    /**
     * 添加黑名单
     * 
     * @param blacklist 黑名单
     * @return 是否成功
     */
    public boolean addBlacklist(FirewallBlacklist blacklist) {
        try {
            blacklistMapper.insert(blacklist);
            refreshBlacklist();
            log.info("黑名单添加成功: {}", blacklist.getIpAddress());
            return true;
        } catch (Exception e) {
            log.error("黑名单添加失败: {}", blacklist.getIpAddress(), e);
            return false;
        }
    }
    
    /**
     * 删除黑名单
     * 
     * @param id 黑名单ID
     * @return 是否成功
     */
    public boolean deleteBlacklist(Long id) {
        try {
            blacklistMapper.deleteById(id);
            refreshBlacklist();
            log.info("黑名单删除成功: {}", id);
            return true;
        } catch (Exception e) {
            log.error("黑名单删除失败: {}", id, e);
            return false;
        }
    }
    
    /**
     * 添加白名单
     * 
     * @param whitelist 白名单
     * @return 是否成功
     */
    public boolean addWhitelist(FirewallWhitelist whitelist) {
        try {
            whitelistMapper.insert(whitelist);
            refreshWhitelist();
            log.info("白名单添加成功: {}", whitelist.getIpAddress());
            return true;
        } catch (Exception e) {
            log.error("白名单添加失败: {}", whitelist.getIpAddress(), e);
            return false;
        }
    }
    
    /**
     * 删除白名单
     * 
     * @param id 白名单ID
     * @return 是否成功
     */
    public boolean deleteWhitelist(Long id) {
        try {
            whitelistMapper.deleteById(id);
            refreshWhitelist();
            log.info("白名单删除成功: {}", id);
            return true;
        } catch (Exception e) {
            log.error("白名单删除失败: {}", id, e);
            return false;
        }
    }
    
    /**
     * 更新黑名单
     * 
     * @param blacklist 黑名单
     * @return 是否成功
     */
    public boolean updateBlacklist(FirewallBlacklist blacklist) {
        try {
            blacklistMapper.update(blacklist);
            refreshBlacklist();
            log.info("黑名单更新成功: {}", blacklist.getIpAddress());
            return true;
        } catch (Exception e) {
            log.error("黑名单更新失败: {}", blacklist.getIpAddress(), e);
            return false;
        }
    }
    
    /**
     * 更新白名单
     * 
     * @param whitelist 白名单
     * @return 是否成功
     */
    public boolean updateWhitelist(FirewallWhitelist whitelist) {
        try {
            whitelistMapper.update(whitelist);
            refreshWhitelist();
            log.info("白名单更新成功: {}", whitelist.getIpAddress());
            return true;
        } catch (Exception e) {
            log.error("白名单更新失败: {}", whitelist.getIpAddress(), e);
            return false;
        }
    }
    
    /**
     * 刷新规则缓存
     */
    public void refreshRules() {
        try {
            List<FirewallRule> rules = ruleMapper.findAllEnabled();
            ruleCache.clear();
            ruleCache.putAll(rules.stream()
                    .collect(Collectors.toMap(FirewallRule::getApiPattern, rule -> rule, (old, new_) -> new_)));
            log.debug("规则缓存刷新完成，共加载 {} 条规则", ruleCache.size());
        } catch (Exception e) {
            log.error("刷新规则缓存失败", e);
        }
    }
    
    /**
     * 刷新黑名单缓存
     */
    public void refreshBlacklist() {
        try {
            List<FirewallBlacklist> blacklists = blacklistMapper.findAllValid();
            blacklistCache.clear();
            blacklistCache.putAll(blacklists.stream()
                    .collect(Collectors.toMap(FirewallBlacklist::getIpAddress, blacklist -> blacklist, (old, new_) -> new_)));
            log.debug("黑名单缓存刷新完成，共加载 {} 条记录", blacklistCache.size());
        } catch (Exception e) {
            log.error("刷新黑名单缓存失败", e);
        }
    }
    
    /**
     * 刷新白名单缓存
     */
    public void refreshWhitelist() {
        try {
            List<FirewallWhitelist> whitelists = whitelistMapper.findAllValid();
            whitelistCache.clear();
            whitelistCache.putAll(whitelists.stream()
                    .collect(Collectors.toMap(FirewallWhitelist::getIpAddress, whitelist -> whitelist, (old, new_) -> new_)));
            log.debug("白名单缓存刷新完成，共加载 {} 条记录", whitelistCache.size());
        } catch (Exception e) {
            log.error("刷新白名单缓存失败", e);
        }
    }
    
    /**
     * 定时刷新缓存（每5分钟）
     */
    @Scheduled(fixedRate = 300000)
    public void scheduledRefresh() {
        log.debug("定时刷新防火墙规则缓存...");
        refreshRules();
        refreshBlacklist();
        refreshWhitelist();
        
        // 清理过期的黑名单
        try {
            int cleaned = blacklistMapper.cleanExpired();
            if (cleaned > 0) {
                log.info("清理过期黑名单 {} 条", cleaned);
                refreshBlacklist();
            }
        } catch (Exception e) {
            log.error("清理过期黑名单失败", e);
        }
    }
    
    /**
     * 获取缓存统计信息
     * 
     * @return 统计信息
     */
    public Map<String, Object> getCacheStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("ruleCount", ruleCache.size());
        stats.put("blacklistCount", blacklistCache.size());
        stats.put("whitelistCount", whitelistCache.size());
        return stats;
    }
}