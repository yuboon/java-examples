package com.example.netcapture.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.pcap4j.core.PcapNetworkInterface;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.netcapture.dto.CaptureControlDto;
import com.example.netcapture.service.PacketCaptureService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/capture")
@RequiredArgsConstructor
public class CaptureController {
    
    private final PacketCaptureService captureService;
    
    @GetMapping("/interfaces")
    public ResponseEntity<Map<String, Object>> getNetworkInterfaces() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<PcapNetworkInterface> interfaces = captureService.getAvailableNetworkInterfaces();
            
            List<Map<String, Object>> interfaceList = interfaces.stream()
                .map(nif -> {
                    Map<String, Object> interfaceInfo = new HashMap<>();
                    interfaceInfo.put("name", nif.getName());
                    interfaceInfo.put("description", nif.getDescription());
                    interfaceInfo.put("addresses", nif.getAddresses().stream()
                        .map(addr -> addr.getAddress() != null ? addr.getAddress().getHostAddress() : "")
                        .filter(addr -> !addr.isEmpty())
                        .toList());
                    return interfaceInfo;
                })
                .toList();
            
            result.put("success", true);
            result.put("interfaces", interfaceList);
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error getting network interfaces", e);
            result.put("success", false);
            result.put("error", "获取网络接口失败");
            result.put("message", "请确保已安装 Npcap 并以管理员权限运行应用");
            result.put("downloadUrl", "https://npcap.com/");
            return ResponseEntity.ok(result);
        }
    }
    
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getCaptureStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("isCapturing", captureService.isCapturing());
        status.put("capturedPackets", captureService.getCapturedPacketsCount());
        
        return ResponseEntity.ok(status);
    }
    
    @PostMapping("/control")
    public ResponseEntity<Map<String, Object>> controlCapture(@RequestBody CaptureControlDto controlDto) {
        Map<String, Object> response = new HashMap<>();
        
        try {
            switch (controlDto.getAction().toLowerCase()) {
                case "start":
                    if (captureService.isCapturing()) {
                        response.put("success", false);
                        response.put("message", "Capture is already running");
                    } else {
                        String networkInterface = controlDto.getNetworkInterface();
                        String filter = controlDto.getFilter();
                        
                        if (networkInterface != null && !networkInterface.trim().isEmpty()) {
                            captureService.startCapture(networkInterface, filter);
                        } else {
                            captureService.startCapture();
                        }
                        
                        response.put("success", true);
                        response.put("message", "Capture started successfully");
                    }
                    break;
                    
                case "stop":
                    captureService.stopCapture();
                    response.put("success", true);
                    response.put("message", "Capture stopped successfully");
                    break;
                    
                default:
                    response.put("success", false);
                    response.put("message", "Unknown action: " + controlDto.getAction());
                    break;
            }
        } catch (Exception e) {
            log.error("Error controlling capture", e);
            response.put("success", false);
            response.put("message", "Error: " + e.getMessage());
        }
        
        return ResponseEntity.ok(response);
    }
}