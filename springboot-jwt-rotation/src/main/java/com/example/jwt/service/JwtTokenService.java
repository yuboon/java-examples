package com.example.jwt.service;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Base64;

/**
 * JWT Token 服务
 * 负责Token的生成、验证和解析
 */
@Service
public class JwtTokenService {

    private static final Logger logger = LoggerFactory.getLogger(JwtTokenService.class);

    @Autowired
    private DynamicKeyStore keyStore;

    @Value("${jwt.token-expiration:24}")
    private int tokenExpirationHours;

    /**
     * 生成JWT Token
     * @param username 用户名
     * @param claims 额外的声明信息
     * @return JWT Token字符串
     */
    public String generateToken(String username, Map<String, Object> claims) {
        try {
            // 获取当前活跃密钥
            var currentKey = keyStore.getCurrentKey();
            String keyId = currentKey.getKeyId();

            // 构建JWT
            JwtBuilder builder = Jwts.builder()
                    .subject(username)
                    .issuedAt(new Date())
                    .expiration(Date.from(Instant.now().plus(tokenExpirationHours, ChronoUnit.HOURS)))
                    .header().keyId(keyId).and()
                    .signWith(currentKey.getKeyPair().getPrivate(), Jwts.SIG.RS256);

            // 添加额外声明
            if (claims != null && !claims.isEmpty()) {
                builder.claims().add(claims);
            }

            String token = builder.compact();

            logger.info("为用户 {} 生成JWT Token, 使用密钥: {}", username, keyId);
            return token;

        } catch (Exception e) {
            logger.error("生成JWT Token失败", e);
            throw new RuntimeException("JWT Token生成失败", e);
        }
    }

    /**
     * 生成JWT Token（简化版本）
     * @param username 用户名
     * @return JWT Token字符串
     */
    public String generateToken(String username) {
        return generateToken(username, null);
    }

    /**
     * 验证并解析JWT Token
     * @param token JWT Token字符串
     * @return 解析后的Claims
     * @throws JwtException Token无效或过期
     */
    public Claims validateToken(String token) throws JwtException {
        try {
            // 首先解析Header获取密钥ID
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new JwtException("Token格式错误");
            }

            // 解析Header部分
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> headerMap = mapper.readValue(headerJson, Map.class);
            String keyId = (String) headerMap.get("kid");

            if (keyId == null) {
                throw new JwtException("Token缺少密钥ID (kid)");
            }

            // 获取对应的公钥
            var keyInfo = keyStore.getKey(keyId);
            if (keyInfo == null) {
                throw new JwtException("找不到对应的密钥: " + keyId);
            }

            PublicKey publicKey = keyInfo.getKeyPair().getPublic();

            // 使用公钥验证Token
            Jws<Claims> jws = Jwts.parser()
                    .verifyWith(publicKey)
                    .build()
                    .parseSignedClaims(token);

            Claims claims = jws.getPayload();
            logger.info("Token验证成功, 用户: {}, 密钥: {}", claims.getSubject(), keyId);

            return claims;

        } catch (ExpiredJwtException e) {
            logger.warn("Token已过期: {}", e.getMessage());
            throw new JwtException("Token已过期", e);
        } catch (UnsupportedJwtException e) {
            logger.warn("不支持的Token格式: {}", e.getMessage());
            throw new JwtException("不支持的Token格式", e);
        } catch (MalformedJwtException e) {
            logger.warn("Token格式错误: {}", e.getMessage());
            throw new JwtException("Token格式错误", e);
        } catch (SignatureException e) {
            logger.warn("Token签名验证失败: {}", e.getMessage());
            throw new JwtException("Token签名验证失败", e);
        } catch (IllegalArgumentException e) {
            logger.warn("Token参数错误: {}", e.getMessage());
            throw new JwtException("Token参数错误", e);
        } catch (Exception e) {
            logger.error("Token验证过程中发生未知错误", e);
            throw new JwtException("Token验证失败", e);
        }
    }

    /**
     * 检查Token是否即将过期（1小时内）
     * @param token JWT Token字符串
     * @return true如果即将过期
     */
    public boolean isTokenExpiringSoon(String token) {
        try {
            Claims claims = validateToken(token);
            Date expiration = claims.getExpiration();
            Instant oneHourFromNow = Instant.now().plus(1, ChronoUnit.HOURS);
            return expiration.toInstant().isBefore(oneHourFromNow);
        } catch (JwtException e) {
            // 如果Token无效，也认为需要刷新
            return true;
        }
    }

    /**
     * 刷新Token（生成新的Token）
     * @param token 旧Token
     * @return 新Token
     * @throws JwtException 如果旧Token无效
     */
    public String refreshToken(String token) throws JwtException {
        Claims claims = validateToken(token);
        String username = claims.getSubject();

        // 保留原有的非标准声明（除了时间相关的）
        Map<String, Object> newClaims = new HashMap<>();
        for (Map.Entry<String, Object> entry : claims.entrySet()) {
            String key = entry.getKey();
            if (!"exp".equals(key) && !"iat".equals(key) && !"nbf".equals(key)) {
                newClaims.put(key, entry.getValue());
            }
        }

        logger.info("为用户 {} 刷新Token", username);
        return generateToken(username, newClaims);
    }

    /**
     * 从Token中提取用户名（不验证签名）
     * @param token JWT Token字符串
     * @return 用户名
     */
    public String extractUsername(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            // 解析Payload部分
            String payloadJson = new String(Base64.getUrlDecoder().decode(parts[1]));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> payloadMap = mapper.readValue(payloadJson, Map.class);
            return (String) payloadMap.get("sub");
        } catch (Exception e) {
            logger.warn("提取用户名失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 获取Token的密钥ID（不验证签名）
     * @param token JWT Token字符串
     * @return 密钥ID
     */
    public String extractKeyId(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                return null;
            }

            // 解析Header部分
            String headerJson = new String(Base64.getUrlDecoder().decode(parts[0]));
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            Map<String, Object> headerMap = mapper.readValue(headerJson, Map.class);
            return (String) headerMap.get("kid");
        } catch (Exception e) {
            logger.warn("提取密钥ID失败: {}", e.getMessage());
            return null;
        }
    }
}