package com.example.netcapture.service;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import com.example.netcapture.entity.PacketInfo;
import com.example.netcapture.repository.PacketInfoMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class PacketAnalysisService {
    
    private final PacketInfoMapper packetInfoMapper;
    private final ProtocolAnalyzer protocolAnalyzer;
    private final TrafficStatisticsService statisticsService;
    private final ThreadPoolTaskExecutor analysisTaskExecutor;
    
    public PacketAnalysisService(PacketInfoMapper packetInfoMapper,
                               ProtocolAnalyzer protocolAnalyzer,
                               TrafficStatisticsService statisticsService,
                               @Qualifier("analysisTaskExecutor") ThreadPoolTaskExecutor analysisTaskExecutor) {
        this.packetInfoMapper = packetInfoMapper;
        this.protocolAnalyzer = protocolAnalyzer;
        this.statisticsService = statisticsService;
        this.analysisTaskExecutor = analysisTaskExecutor;
    }
    
    public void analyzePacket(PacketInfo packetInfo) {
        analysisTaskExecutor.execute(() -> {
            try {
                enhancePacketInfo(packetInfo);
                savePacket(packetInfo);
                statisticsService.updateStatistics(packetInfo);
            } catch (Exception e) {
                log.error("Error analyzing packet", e);
            }
        });
    }
    
    private void enhancePacketInfo(PacketInfo packetInfo) {
        if ("TCP".equals(packetInfo.getProtocol())) {
            protocolAnalyzer.analyzeTcpPacket(packetInfo);
        } else if ("UDP".equals(packetInfo.getProtocol())) {
            protocolAnalyzer.analyzeUdpPacket(packetInfo);
        }
    }
    
    private void savePacket(PacketInfo packetInfo) {
        try {
            packetInfoMapper.insert(packetInfo);
        } catch (Exception e) {
            log.error("Error saving packet to database", e);
        }
    }
}