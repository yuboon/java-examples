package com.example.login.service.impl;

import com.example.login.config.LoginProperties;
import com.example.login.model.LoginInfo;
import com.example.login.model.TokenInfo;
import com.example.login.service.SessionManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Map的会话管理器实现
 */
@Slf4j
@Service
public class MapSessionManager implements SessionManager {

    private final LoginProperties properties;

    // 用户名 -> Token列表
    private final Map<String, Set<String>> userTokenMap = new ConcurrentHashMap<>();

    // Token -> Token信息
    private final Map<String, TokenInfo> tokenMap = new ConcurrentHashMap<>();

    public MapSessionManager(LoginProperties properties) {
        this.properties = properties;
    }

    @Override
    public String login(String username, LoginInfo loginInfo) {
        // 生成Token
        String token = generateToken();

        // 设置登录时间
        if (loginInfo.getLoginTime() == 0) {
            loginInfo.setLoginTime(System.currentTimeMillis());
        }

        // 创建Token信息
        TokenInfo tokenInfo = new TokenInfo(
            token,
            username,
            loginInfo,
            System.currentTimeMillis() + properties.getTokenExpireTime() * 1000
        );

        // 根据登录模式处理
        if (properties.getMode() == LoginProperties.LoginMode.SINGLE) {
            // 单用户单登录：先踢出旧Token
            kickoutUser(username);
        }

        // 保存Token
        tokenMap.put(token, tokenInfo);
        userTokenMap.computeIfAbsent(username, k -> ConcurrentHashMap.newKeySet())
                   .add(token);

        log.info("用户登录成功: username={}, token={}, mode={}",
            username, token, properties.getMode());

        return token;
    }

    @Override
    public void logout(String token) {
        TokenInfo tokenInfo = tokenMap.remove(token);
        if (tokenInfo != null) {
            Set<String> tokens = userTokenMap.get(tokenInfo.getUsername());
            if (tokens != null) {
                tokens.remove(token);
                if (tokens.isEmpty()) {
                    userTokenMap.remove(tokenInfo.getUsername());
                }
            }
            log.info("用户登出: username={}, token={}",
                tokenInfo.getUsername(), token);
        }
    }

    @Override
    public TokenInfo validateToken(String token) {
        TokenInfo tokenInfo = tokenMap.get(token);
        if (tokenInfo == null) {
            return null;
        }

        // 检查是否过期
        if (System.currentTimeMillis() > tokenInfo.getExpireTime()) {
            logout(token);
            return null;
        }

        // 更新过期时间（续期）
        tokenInfo.setExpireTime(
            System.currentTimeMillis() + properties.getTokenExpireTime() * 1000
        );

        return tokenInfo;
    }

    @Override
    public List<String> getUserTokens(String username) {
        Set<String> tokens = userTokenMap.get(username);
        return tokens != null ? new ArrayList<>(tokens) : Collections.emptyList();
    }

    @Override
    public void kickoutUser(String username) {
        Set<String> tokens = userTokenMap.remove(username);
        if (tokens != null) {
            for (String token : tokens) {
                tokenMap.remove(token);
            }
            log.info("踢出用户所有会话: username={}", username);
        }
    }

    @Override
    public Set<String> getOnlineUsers() {
        return new HashSet<>(userTokenMap.keySet());
    }

    @Override
    public void cleanExpiredTokens() {
        long now = System.currentTimeMillis();
        List<String> expiredTokens = new ArrayList<>();

        tokenMap.forEach((token, info) -> {
            if (now > info.getExpireTime()) {
                expiredTokens.add(token);
            }
        });

        expiredTokens.forEach(this::logout);
        log.info("清理过期Token: {}个", expiredTokens.size());
    }

    /**
     * 生成Token
     */
    private String generateToken() {
        return properties.getTokenPrefix() +
            UUID.randomUUID().toString().replace("-", "");
    }
}