package com.example.face2face.service;

import com.example.face2face.dto.request.RegisterRequest;
import com.example.face2face.dto.response.UserResponse;
import com.example.face2face.exception.AppException;
import com.example.face2face.model.User;
import com.example.face2face.util.RedisKeyUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private LocationService locationService;

    @Autowired
    private ObjectMapper objectMapper;

    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public User registerUser(RegisterRequest request) {
        // Check if username already exists
        String existingUserId = stringRedisTemplate.opsForValue().get(RedisKeyUtil.getUsernameKey(request.getUsername()));
        if (existingUserId != null) {
            throw new AppException(400, "Username already exists");
        }

        // Create new user
        String userId = UUID.randomUUID().toString();
        User user = new User();
        user.setId(userId);
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar() != null ? request.getAvatar() : "default.png");
        user.setCreateTime(LocalDateTime.now());

        // Save user to Redis
        Map<String, Object> userMap = new HashMap<>();
        userMap.put("id", user.getId());
        userMap.put("username", user.getUsername());
        userMap.put("password", user.getPassword());
        userMap.put("nickname", user.getNickname());
        userMap.put("avatar", user.getAvatar());
        userMap.put("createTime", user.getCreateTime().toString());

        redisTemplate.opsForHash().putAll(RedisKeyUtil.getUserKey(userId), userMap);
        stringRedisTemplate.opsForValue().set(RedisKeyUtil.getUsernameKey(user.getUsername()), userId);

        return user;
    }

    
    public User getUserById(String id) {
        Map<Object, Object> userMap = redisTemplate.opsForHash().entries(RedisKeyUtil.getUserKey(id));
        if (userMap.isEmpty()) {
            return null;
        }

        return mapToUser(userMap);
    }

    
    public User getUserByUsername(String username) {
        String userId = stringRedisTemplate.opsForValue().get(RedisKeyUtil.getUsernameKey(username));
        if (userId == null) {
            return null;
        }

        return getUserById(userId);
    }

    
    public List<UserResponse> getNearbyUsers(double latitude, double longitude, double radius) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = locationService.getNearbyLocations(
                RedisKeyUtil.USER_LOCATIONS, latitude, longitude, radius);

        List<UserResponse> nearbyUsers = new ArrayList<>();
        for (var result : results) {
            String userId = result.getContent().getName();
            User user = getUserById(userId);
            if (user != null) {
                UserResponse userResponse = convertToUserResponse(user);
                userResponse.setDistance(result.getDistance().getValue());
                nearbyUsers.add(userResponse);
            }
        }

        return nearbyUsers;
    }

    
    public List<UserResponse> getGroupMembers(String groupId) {
        Set<String> memberIds = stringRedisTemplate.opsForSet().members(RedisKeyUtil.getGroupMembersKey(groupId));
        if (memberIds == null || memberIds.isEmpty()) {
            return Collections.emptyList();
        }

        return memberIds.stream()
                .map(this::getUserById)
                .filter(Objects::nonNull)
                .map(this::convertToUserResponse)
                .collect(Collectors.toList());
    }

    
    public UserResponse convertToUserResponse(User user) {
        if (user == null) {
            return null;
        }

        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setNickname(user.getNickname());
        response.setAvatar(user.getAvatar());
        return response;
    }

    private User mapToUser(Map<Object, Object> userMap) {
        User user = new User();
        user.setId((String) userMap.get("id"));
        user.setUsername((String) userMap.get("username"));
        user.setPassword((String) userMap.get("password"));
        user.setNickname((String) userMap.get("nickname"));
        user.setAvatar((String) userMap.get("avatar"));
        user.setCreateTime(LocalDateTime.parse((String) userMap.get("createTime")));
        return user;
    }
}