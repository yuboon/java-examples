// GroupService.java
package com.example.face2face.service;

import com.example.face2face.dto.request.CreateGroupRequest;
import com.example.face2face.dto.response.GroupResponse;
import com.example.face2face.dto.response.UserResponse;
import com.example.face2face.exception.AppException;
import com.example.face2face.model.Group;
import com.example.face2face.model.User;
import com.example.face2face.service.LocationService;
import com.example.face2face.service.UserService;
import com.example.face2face.util.RedisKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Point;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private LocationService locationService;

    @Autowired
    private UserService userService;

    @Value("${face2face.distanceThresholdMeters}")
    private double distanceThresholdMeters;

    private static final long GROUP_EXPIRY_HOURS = 24;

    
    public GroupResponse createGroup(CreateGroupRequest request, User creator) {
        // Generate unique group ID and invitation code
        String groupId = UUID.randomUUID().toString();
        String invitationCode = generateInvitationCode();

        // Create group
        Group group = new Group();
        group.setId(groupId);
        group.setName(request.getName());
        group.setDescription(request.getDescription());
        group.setCreatorId(creator.getId());
        group.setLatitude(request.getLatitude());
        group.setLongitude(request.getLongitude());
        group.setInvitationCode(invitationCode);
        group.setCreateTime(LocalDateTime.now());
        group.setExpireTime(LocalDateTime.now().plusHours(GROUP_EXPIRY_HOURS));

        // Save group to Redis
        Map<String, Object> groupMap = new HashMap<>();
        groupMap.put("id", group.getId());
        groupMap.put("name", group.getName());
        groupMap.put("description", group.getDescription());
        groupMap.put("creatorId", group.getCreatorId());
        groupMap.put("latitude", group.getLatitude());
        groupMap.put("longitude", group.getLongitude());
        groupMap.put("invitationCode", group.getInvitationCode());
        groupMap.put("createTime", group.getCreateTime().toString());
        groupMap.put("expireTime", group.getExpireTime().toString());

        redisTemplate.opsForHash().putAll(RedisKeyUtil.getGroupKey(groupId), groupMap);
        stringRedisTemplate.opsForValue().set(
                RedisKeyUtil.getInvitationCodeKey(invitationCode), 
                groupId,
                java.time.Duration.ofHours(GROUP_EXPIRY_HOURS)
        );

        // Save group location
        locationService.updateLocation(
                RedisKeyUtil.GROUP_LOCATIONS,
                groupId,
                group.getLatitude(),
                group.getLongitude()
        );

        // Creator joins the group
        joinGroup(groupId, creator.getId());

        // Return response
        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        response.setCreator(userService.convertToUserResponse(creator));
        response.setLatitude(group.getLatitude());
        response.setLongitude(group.getLongitude());
        response.setInvitationCode(group.getInvitationCode());
        response.setCreateTime(group.getCreateTime());
        response.setMemberCount(1);

        return response;
    }

    
    public Group getGroupById(String groupId) {
        Map<Object, Object> groupMap = redisTemplate.opsForHash().entries(RedisKeyUtil.getGroupKey(groupId));
        if (groupMap.isEmpty()) {
            return null;
        }

        return mapToGroup(groupMap);
    }

    
    public GroupResponse getGroupResponse(String groupId) {
        Group group = getGroupById(groupId);
        if (group == null) {
            return null;
        }

        GroupResponse response = new GroupResponse();
        response.setId(group.getId());
        response.setName(group.getName());
        response.setDescription(group.getDescription());
        
        User creator = userService.getUserById(group.getCreatorId());
        response.setCreator(userService.convertToUserResponse(creator));
        
        response.setLatitude(group.getLatitude());
        response.setLongitude(group.getLongitude());
        response.setInvitationCode(group.getInvitationCode());
        response.setCreateTime(group.getCreateTime());
        
        // Get members
        List<UserResponse> members = userService.getGroupMembers(groupId);
        response.setMembers(members);
        response.setMemberCount(members.size());

        return response;
    }

    
    public List<GroupResponse> getNearbyGroups(double latitude, double longitude, double radius) {
        GeoResults<RedisGeoCommands.GeoLocation<String>> results = locationService.getNearbyLocations(
                RedisKeyUtil.GROUP_LOCATIONS, latitude, longitude, radius);

        List<GroupResponse> nearbyGroups = new ArrayList<>();
        for (var result : results) {
            String groupId = result.getContent().getName();
            Group group = getGroupById(groupId);
            
            if (group != null && group.getExpireTime().isAfter(LocalDateTime.now())) {
                GroupResponse response = new GroupResponse();
                response.setId(group.getId());
                response.setName(group.getName());
                response.setDescription(group.getDescription());
                
                User creator = userService.getUserById(group.getCreatorId());
                response.setCreator(userService.convertToUserResponse(creator));
                
                response.setLatitude(group.getLatitude());
                response.setLongitude(group.getLongitude());
                response.setCreateTime(group.getCreateTime());
                response.setDistance(result.getDistance().getValue());
                
                // Get member count
                Long memberCount = stringRedisTemplate.opsForSet().size(RedisKeyUtil.getGroupMembersKey(groupId));
                response.setMemberCount(memberCount != null ? memberCount.intValue() : 0);
                
                nearbyGroups.add(response);
            }
        }

        return nearbyGroups;
    }

    
    public List<GroupResponse> getUserGroups(String userId) {
        Set<String> groupIds = stringRedisTemplate.opsForSet().members(RedisKeyUtil.getUserGroupsKey(userId));
        if (groupIds == null || groupIds.isEmpty()) {
            return Collections.emptyList();
        }

        return groupIds.stream()
                .map(this::getGroupById)
                .filter(group -> group != null && group.getExpireTime().isAfter(LocalDateTime.now()))
                .map(group -> {
                    GroupResponse response = new GroupResponse();
                    response.setId(group.getId());
                    response.setName(group.getName());
                    response.setDescription(group.getDescription());
                    
                    User creator = userService.getUserById(group.getCreatorId());
                    response.setCreator(userService.convertToUserResponse(creator));
                    
                    response.setLatitude(group.getLatitude());
                    response.setLongitude(group.getLongitude());
                    response.setCreateTime(group.getCreateTime());
                    
                    // Get member count
                    Long memberCount = stringRedisTemplate.opsForSet().size(RedisKeyUtil.getGroupMembersKey(group.getId()));
                    response.setMemberCount(memberCount != null ? memberCount.intValue() : 0);
                    
                    return response;
                })
                .collect(Collectors.toList());
    }

    
    public boolean joinGroup(String groupId, String userId) {
        Group group = getGroupById(groupId);
        if (group == null || group.getExpireTime().isBefore(LocalDateTime.now())) {
            return false;
        }

        List<Point> userPoints = locationService.getUserLocation(RedisKeyUtil.USER_LOCATIONS,userId);
        if (userPoints.isEmpty()) {
            throw new AppException(400,"User location not found");
        }

        double distance = locationService.calculateDistance(
                userPoints.get(0).getY(),userPoints.get(0).getX(),
                group.getLatitude(), group.getLongitude()
        );

        if (distance > distanceThresholdMeters) {
            throw new AppException(400,"You are too far from the group creator");
        }

        // Add user to group members
        stringRedisTemplate.opsForSet().add(RedisKeyUtil.getGroupMembersKey(groupId), userId);
        
        // Add group to user's groups
        stringRedisTemplate.opsForSet().add(RedisKeyUtil.getUserGroupsKey(userId), groupId);
        
        return true;
    }


    
    public boolean joinGroupByInvitationCode(String invitationCode, String userId) {
        String groupId = stringRedisTemplate.opsForValue().get(RedisKeyUtil.getInvitationCodeKey(invitationCode));
        if (groupId == null) {
            return false;
        }

        return joinGroup(groupId, userId);
    }

    
    public boolean isGroupMember(String groupId, String userId) {
        return Boolean.TRUE.equals(
                stringRedisTemplate.opsForSet().isMember(RedisKeyUtil.getGroupMembersKey(groupId), userId));
    }

    private String generateInvitationCode() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder code = new StringBuilder();
        Random random = new Random();
        for (int i = 0; i < 6; i++) {
            code.append(chars.charAt(random.nextInt(chars.length())));
        }
        return code.toString();
    }

    private Group mapToGroup(Map<Object, Object> groupMap) {
        Group group = new Group();
        group.setId((String) groupMap.get("id"));
        group.setName((String) groupMap.get("name"));
        group.setDescription((String) groupMap.get("description"));
        group.setCreatorId((String) groupMap.get("creatorId"));
        group.setLatitude(Double.parseDouble(groupMap.get("latitude").toString()));
        group.setLongitude(Double.parseDouble(groupMap.get("longitude").toString()));
        group.setInvitationCode((String) groupMap.get("invitationCode"));
        group.setCreateTime(LocalDateTime.parse((String) groupMap.get("createTime")));
        group.setExpireTime(LocalDateTime.parse((String) groupMap.get("expireTime")));
        return group;
    }
}