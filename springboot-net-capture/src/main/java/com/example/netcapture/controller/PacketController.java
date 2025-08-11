package com.example.netcapture.controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.netcapture.dto.PacketInfoDto;
import com.example.netcapture.entity.PacketInfo;
import com.example.netcapture.service.PacketQueryService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/packets")
@RequiredArgsConstructor
public class PacketController {
    
    private final PacketQueryService packetQueryService;
    
    @GetMapping
    public ResponseEntity<Page<PacketInfoDto>> queryPackets(
            @RequestParam(required = false) String protocol,
            @RequestParam(required = false) String sourceIp,
            @RequestParam(required = false) String destinationIp,
            @RequestParam(required = false) Integer sourcePort,
            @RequestParam(required = false) Integer destinationPort,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime startTime,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime endTime,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "20") int pageSize) {
        
        try {
            Page<PacketInfoDto> result = packetQueryService.queryPackets(
                protocol, sourceIp, destinationIp, sourcePort, destinationPort,
                startTime, endTime, pageNo, pageSize
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            log.error("Error querying packets", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/recent")
    public ResponseEntity<List<PacketInfo>> getRecentPackets(
            @RequestParam(defaultValue = "100") int limit) {
        try {
            List<PacketInfo> packets = packetQueryService.getRecentPackets(limit);
            return ResponseEntity.ok(packets);
        } catch (Exception e) {
            log.error("Error getting recent packets", e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<PacketInfo> getPacketDetail(@PathVariable Long id) {
        try {
            PacketInfo packet = packetQueryService.getPacketDetail(id);
            if (packet != null) {
                return ResponseEntity.ok(packet);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error getting packet detail", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}