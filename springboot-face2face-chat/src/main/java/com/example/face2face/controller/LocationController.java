package com.example.face2face.controller;

import com.example.face2face.dto.request.LocationRequest;
import com.example.face2face.dto.response.ApiResponse;
import com.example.face2face.dto.response.UserResponse;
import com.example.face2face.model.User;
import com.example.face2face.service.LocationService;
import com.example.face2face.service.UserService;
import com.example.face2face.util.RedisKeyUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/location")
public class LocationController {

    @Autowired
    private LocationService locationService;

    @Autowired
    private UserService userService;

    @PostMapping("/update")
    public ApiResponse<Void> updateLocation(
            @RequestBody LocationRequest request,
            HttpServletRequest httpRequest) {
        
        User currentUser = (User) httpRequest.getAttribute("currentUser");
        locationService.updateLocation(
                RedisKeyUtil.USER_LOCATIONS,
                currentUser.getId(),
                request.getLatitude(),
                request.getLongitude()
        );
        
        return ApiResponse.success();
    }

    @GetMapping("/nearby-users")
    public ApiResponse<List<UserResponse>> getNearbyUsers(
            @RequestParam double latitude,
            @RequestParam double longitude,
            @RequestParam(defaultValue = "100") double radius,
            HttpServletRequest httpRequest) {
        
        User currentUser = (User) httpRequest.getAttribute("currentUser");
        List<UserResponse> nearbyUsers = userService.getNearbyUsers(latitude, longitude, radius);
        
        // Remove current user from the list
        nearbyUsers.removeIf(user -> user.getId().equals(currentUser.getId()));
        
        return ApiResponse.success(nearbyUsers);
    }
}