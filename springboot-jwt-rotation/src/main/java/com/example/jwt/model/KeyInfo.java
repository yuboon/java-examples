package com.example.jwt.model;

import java.security.KeyPair;
import java.time.LocalDateTime;

/**
 * 密钥信息封装类
 */
public class KeyInfo {
    private final String keyId;
    private final KeyPair keyPair;
    private final LocalDateTime createdAt;
    private LocalDateTime lastUsed;
    private boolean isActive;

    public KeyInfo(String keyId, KeyPair keyPair) {
        this.keyId = keyId;
        this.keyPair = keyPair;
        this.createdAt = LocalDateTime.now();
        this.lastUsed = LocalDateTime.now();
        this.isActive = true;
    }

    // Getters
    public String getKeyId() { return keyId; }
    public KeyPair getKeyPair() { return keyPair; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getLastUsed() { return lastUsed; }
    public boolean isActive() { return isActive; }

    public void setLastUsed(LocalDateTime lastUsed) { this.lastUsed = lastUsed; }
    public void setActive(boolean active) { isActive = active; }

    @Override
    public String toString() {
        return "KeyInfo{" +
                "keyId='" + keyId + '\'' +
                ", createdAt=" + createdAt +
                ", lastUsed=" + lastUsed +
                ", isActive=" + isActive +
                '}';
    }
}