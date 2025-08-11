package com.example.netcapture.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.netcapture.entity.TrafficStatistics;
import com.example.netcapture.service.TrafficStatisticsService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    
    private final TrafficStatisticsService statisticsService;
    
    @GetMapping("/current")
    public ResponseEntity<Map<String, Object>> getCurrentStatistics() {
        try {
            Map<String, Object> statistics = statisticsService.getCurrentStatistics();
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error getting current statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<TrafficStatistics>> getRecentStatistics(
            @RequestParam(defaultValue = "24") int hours,
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<TrafficStatistics> statistics = statisticsService.getRecentStatistics(hours, limit);
            return ResponseEntity.ok(statistics);
        } catch (Exception e) {
            log.error("Error getting recent statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/trend")
    public ResponseEntity<List<java.util.Map<String, Object>>> getTrafficTrend(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            List<java.util.Map<String, Object>> trend = statisticsService.getTrafficTrend(hours);
            return ResponseEntity.ok(trend);
        } catch (Exception e) {
            log.error("Error getting traffic trend", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/protocols")
    public ResponseEntity<List<java.util.Map<String, Object>>> getProtocolStatistics(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            List<java.util.Map<String, Object>> protocolStats = statisticsService.getProtocolStatistics(hours);
            return ResponseEntity.ok(protocolStats);
        } catch (Exception e) {
            log.error("Error getting protocol statistics", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/top-sources")
    public ResponseEntity<List<java.util.Map<String, Object>>> getTopSourceIps(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            List<java.util.Map<String, Object>> topSources = statisticsService.getTopSourceIps(hours);
            return ResponseEntity.ok(topSources);
        } catch (Exception e) {
            log.error("Error getting top source IPs", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/top-destinations")
    public ResponseEntity<List<java.util.Map<String, Object>>> getTopDestinationIps(
            @RequestParam(defaultValue = "24") int hours) {
        try {
            List<java.util.Map<String, Object>> topDestinations = statisticsService.getTopDestinationIps(hours);
            return ResponseEntity.ok(topDestinations);
        } catch (Exception e) {
            log.error("Error getting top destination IPs", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}