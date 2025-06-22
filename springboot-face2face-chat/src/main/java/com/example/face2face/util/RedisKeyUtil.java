package com.example.face2face.util;

public class RedisKeyUtil {
    // 用户相关键
    public static String getUserKey(String userId) {
        return "user:" + userId;
    }

    public static String getUsernameKey(String username) {
        return "username:" + username;
    }

    public static String getUserGroupsKey(String userId) {
        return "user:groups:" + userId;
    }

    // 位置相关键
    public static final String USER_LOCATIONS = "user:locations";
    public static final String GROUP_LOCATIONS = "group:locations";

    // 群组相关键
    public static String getGroupKey(String groupId) {
        return "group:" + groupId;
    }

    public static String getGroupMembersKey(String groupId) {
        return "group:members:" + groupId;
    }

    public static String getInvitationCodeKey(String code) {
        return "invitation:code:" + code;
    }
}