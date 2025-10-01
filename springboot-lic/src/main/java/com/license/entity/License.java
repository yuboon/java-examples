package com.license.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.time.LocalDate;
import java.util.List;

/**
 * 许可证实体类
 */
@JsonPropertyOrder({"subject", "issuedTo", "hardwareId", "expireAt", "features"})
public class License {

    private String subject;        // 软件名称
    private String issuedTo;       // 授权给谁
    private String hardwareId;     // 硬件指纹（主板序列号）

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate expireAt;    // 到期时间

    private List<String> features; // 功能权限列表
    private String signature;      // 签名（JSON序列化时忽略）

    public License() {}

    public License(String subject, String issuedTo, String hardwareId, LocalDate expireAt, List<String> features) {
        this.subject = subject;
        this.issuedTo = issuedTo;
        this.hardwareId = hardwareId;
        this.expireAt = expireAt;
        this.features = features;
    }

    // Getters and Setters
    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getIssuedTo() {
        return issuedTo;
    }

    public void setIssuedTo(String issuedTo) {
        this.issuedTo = issuedTo;
    }

    public String getHardwareId() {
        return hardwareId;
    }

    public void setHardwareId(String hardwareId) {
        this.hardwareId = hardwareId;
    }

    public LocalDate getExpireAt() {
        return expireAt;
    }

    public void setExpireAt(LocalDate expireAt) {
        this.expireAt = expireAt;
    }

    public List<String> getFeatures() {
        return features;
    }

    public void setFeatures(List<String> features) {
        this.features = features;
    }

    public String getSignature() {
        return signature;
    }

    public void setSignature(String signature) {
        this.signature = signature;
    }

    @Override
    public String toString() {
        return "License{" +
                "subject='" + subject + '\'' +
                ", issuedTo='" + issuedTo + '\'' +
                ", hardwareId='" + hardwareId + '\'' +
                ", expireAt=" + expireAt +
                ", features=" + features +
                '}';
    }
}