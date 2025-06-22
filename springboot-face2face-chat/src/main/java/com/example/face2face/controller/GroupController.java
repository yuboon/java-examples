
// GroupController.java
package com.example.face2face.controller;

import com.example.face2face.dto.request.CreateGroupRequest;
import com.example.face2face.dto.request.JoinGroupRequest;
import com.example.face2face.dto.response.ApiResponse;
import com.example.face2face.dto.response.GroupResponse;
import com.example.face2face.exception.AppException;
import com.example.face2face.model.User;
import com.example.face2face.service.GroupService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/groups")
public class GroupController {

    @Autowired
    private GroupService groupService;

    @PostMapping("/create")
    public ApiResponse<GroupResponse> createGroup(
            @RequestBody CreateGroupRequest request,
            HttpServletRequest httpRequest) {
        
        User currentUser = (User) httpRequest.getAttribute("currentUser");
        GroupResponse group = groupService.createGroup(request, currentUser);
        
        return ApiResponse.success(group);
    }

    @GetMapping("/{groupId}")
    public ApiResponse<GroupResponse> getGroup(
            @PathVariable String groupId,
            HttpServletRequest httpRequest) {
        
        User currentUser = (User) httpRequest.getAttribute("currentUser");
        
        // Check if user is a member of the group
        if (!groupService.isGroupMember(groupId, currentUser.getId())) {
            throw new AppException(403, "You are not a member of this group");
        }
        
        GroupResponse group = groupService.getGroupResponse(groupId);
        if (group == null) {
            throw new AppException(404, "Group not found");
        }
        
        return ApiResponse.success(group);
    }

    @GetMapping("/nearby")
    public ApiResponse<List<GroupResponse>> getNearbyGroups(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "500") double radius) {
        
        List<GroupResponse> nearbyGroups = groupService.getNearbyGroups(latitude, longitude, radius);
        
        return ApiResponse.success(nearbyGroups);
    }

    @GetMapping("/my-groups")
    public ApiResponse<List<GroupResponse>> getMyGroups(HttpServletRequest httpRequest) {
        User currentUser = (User) httpRequest.getAttribute("currentUser");
        List<GroupResponse> userGroups = groupService.getUserGroups(currentUser.getId());
        
        return ApiResponse.success(userGroups);
    }

    @PostMapping("/{groupId}/join")
    public ApiResponse<Boolean> joinGroup(
            @PathVariable String groupId,
            HttpServletRequest httpRequest) {
        
        User currentUser = (User) httpRequest.getAttribute("currentUser");
        boolean joined = groupService.joinGroup(groupId, currentUser.getId());
        
        if (!joined) {
            throw new AppException(400, "Failed to join group");
        }
        
        return ApiResponse.success(true);
    }

    @PostMapping("/join-by-code")
    public ApiResponse<Boolean> joinByCode(
            @RequestBody JoinGroupRequest request,
            HttpServletRequest httpRequest) {
        
        User currentUser = (User) httpRequest.getAttribute("currentUser");
        boolean joined = groupService.joinGroupByInvitationCode(
                request.getInvitationCode(), currentUser.getId());
        
        if (!joined) {
            throw new AppException(400, "Invalid invitation code");
        }
        
        return ApiResponse.success(true);
    }
}