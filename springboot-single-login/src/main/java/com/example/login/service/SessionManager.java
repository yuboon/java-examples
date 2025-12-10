package com.example.login.service;

import com.example.login.model.LoginInfo;
import com.example.login.model.TokenInfo;

import java.util.List;
import java.util.Set;

/**
 * 会话管理接口
 */
public interface SessionManager {

    /**
     * 用户登录
     * @param username 用户名
     * @param loginInfo 登录信息（IP、设备等）
     * @return 登录Token
     */
    String login(String username, LoginInfo loginInfo);

    /**
     * 用户登出
     * @param token 登录Token
     */
    void logout(String token);

    /**
     * 验证Token是否有效
     * @param token 登录Token
     * @return Token信息
     */
    TokenInfo validateToken(String token);

    /**
     * 获取用户的所有Token
     * @param username 用户名
     * @return Token列表
     */
    List<String> getUserTokens(String username);

    /**
     * 踢出用户的所有会话
     * @param username 用户名
     */
    void kickoutUser(String username);

    /**
     * 获取所有在线用户
     * @return 在线用户列表
     */
    Set<String> getOnlineUsers();

    /**
     * 清理过期Token
     */
    void cleanExpiredTokens();
}