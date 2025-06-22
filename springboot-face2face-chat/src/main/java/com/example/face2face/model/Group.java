package com.example.face2face.model;

import java.io.Serializable;
import java.time.LocalDateTime;

public class Group implements Serializable {
    private String id;
    private String name;
    private String description;
    private String creatorId;
    private double latitude;
    private double longitude;
    private String invitationCode;
    private LocalDateTime createTime;
    private LocalDateTime expireTime;

    // Constructors
    public Group() {
    }

    public Group(String id, String name, String description, String creatorId, 
                 double latitude, double longitude, String invitationCode, 
                 LocalDateTime createTime, LocalDateTime expireTime) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.creatorId = creatorId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.invitationCode = invitationCode;
        this.createTime = createTime;
        this.expireTime = expireTime;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCreatorId() {
        return creatorId;
    }

    public void setCreatorId(String creatorId) {
        this.creatorId = creatorId;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getInvitationCode() {
        return invitationCode;
    }

    public void setInvitationCode(String invitationCode) {
        this.invitationCode = invitationCode;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getExpireTime() {
        return expireTime;
    }

    public void setExpireTime(LocalDateTime expireTime) {
        this.expireTime = expireTime;
    }
}