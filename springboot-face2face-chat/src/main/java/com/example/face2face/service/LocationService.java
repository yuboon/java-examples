package com.example.face2face.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.geo.*;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.connection.RedisGeoCommands.GeoLocation;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LocationService {

    @Autowired
    private StringRedisTemplate redisTemplate;

    
    public void updateLocation(String key, String id, double latitude, double longitude) {
        redisTemplate.opsForGeo().add(key, new Point(longitude, latitude), id);
    }

    public List<Point> getUserLocation(String key, String id) {
        return redisTemplate.opsForGeo().position(key,id);
    }
    
    public GeoResults<GeoLocation<String>> getNearbyLocations(String key, double latitude, double longitude, double radius) {
        Circle circle = new Circle(new Point(longitude, latitude), new Distance(radius, Metrics.KILOMETERS));
        RedisGeoCommands.GeoRadiusCommandArgs args = RedisGeoCommands.GeoRadiusCommandArgs
                .newGeoRadiusArgs()
                .includeDistance()
                .sortAscending();

        return redisTemplate.opsForGeo().radius(key, circle, args);
    }

    public double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        // 使用Haversine公式计算两点间的距离（单位：米）
        double earthRadius = 6371000; // 地球半径，单位：米

        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon/2) * Math.sin(dLon/2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));

        return earthRadius * c;
    }

}