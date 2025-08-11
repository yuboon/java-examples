package com.example.netcapture.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.netcapture.dto.PacketInfoDto;
import com.example.netcapture.entity.PacketInfo;
import com.example.netcapture.repository.PacketInfoMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PacketQueryService {
    
    private final PacketInfoMapper packetInfoMapper;
    
    public Page<PacketInfoDto> queryPackets(String protocol, String sourceIp, String destinationIp, 
                                          Integer sourcePort, Integer destinationPort,
                                          LocalDateTime startTime, LocalDateTime endTime,
                                          int pageNo, int pageSize) {
        
        QueryWrapper<PacketInfo> wrapper = new QueryWrapper<>();
        
        if (protocol != null && !protocol.trim().isEmpty()) {
            wrapper.eq("protocol", protocol);
        }
        if (sourceIp != null && !sourceIp.trim().isEmpty()) {
            wrapper.like("source_ip", sourceIp);
        }
        if (destinationIp != null && !destinationIp.trim().isEmpty()) {
            wrapper.like("destination_ip", destinationIp);
        }
        if (sourcePort != null) {
            wrapper.eq("source_port", sourcePort);
        }
        if (destinationPort != null) {
            wrapper.eq("destination_port", destinationPort);
        }
        if (startTime != null) {
            wrapper.ge("capture_time", startTime);
        }
        if (endTime != null) {
            wrapper.le("capture_time", endTime);
        }
        
        wrapper.orderByDesc("capture_time");
        
        Page<PacketInfo> page = new Page<>(pageNo, pageSize);
        Page<PacketInfo> result = packetInfoMapper.selectPage(page, wrapper);
        
        Page<PacketInfoDto> dtoPage = new Page<>();
        dtoPage.setCurrent(result.getCurrent());
        dtoPage.setSize(result.getSize());
        dtoPage.setTotal(result.getTotal());
        dtoPage.setPages(result.getPages());
        
        List<PacketInfoDto> dtoList = result.getRecords().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
        
        dtoPage.setRecords(dtoList);
        
        return dtoPage;
    }
    
    public List<PacketInfo> getRecentPackets(int limit) {
        QueryWrapper<PacketInfo> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("capture_time").last("LIMIT " + limit);
        return packetInfoMapper.selectList(wrapper);
    }
    
    public PacketInfo getPacketDetail(Long id) {
        return packetInfoMapper.selectById(id);
    }
    
    private PacketInfoDto convertToDto(PacketInfo packet) {
        PacketInfoDto dto = new PacketInfoDto();
        dto.setId(packet.getId());
        dto.setCaptureTime(packet.getCaptureTime());
        dto.setSourceIp(packet.getSourceIp());
        dto.setDestinationIp(packet.getDestinationIp());
        dto.setSourcePort(packet.getSourcePort());
        dto.setDestinationPort(packet.getDestinationPort());
        dto.setProtocol(packet.getProtocol());
        dto.setPacketLength(packet.getPacketLength());
        dto.setHttpMethod(packet.getHttpMethod());
        dto.setHttpUrl(packet.getHttpUrl());
        dto.setHttpStatus(packet.getHttpStatus());
        dto.setTcpFlags(packet.getTcpFlags());
        dto.setNetworkInterface(packet.getNetworkInterface());
        
        dto.setSummary(generatePacketSummary(packet));
        
        return dto;
    }
    
    private String generatePacketSummary(PacketInfo packet) {
        StringBuilder summary = new StringBuilder();
        
        summary.append(packet.getProtocol());
        
        if (packet.getSourceIp() != null && packet.getDestinationIp() != null) {
            summary.append(" ")
                   .append(packet.getSourceIp())
                   .append(":")
                   .append(packet.getSourcePort() != null ? packet.getSourcePort() : "")
                   .append(" -> ")
                   .append(packet.getDestinationIp())
                   .append(":")
                   .append(packet.getDestinationPort() != null ? packet.getDestinationPort() : "");
        }
        
        if (packet.getHttpMethod() != null) {
            summary.append(" [").append(packet.getHttpMethod()).append("]");
        }
        
        if (packet.getHttpUrl() != null) {
            summary.append(" ").append(packet.getHttpUrl());
        }
        
        if (packet.getHttpStatus() != null) {
            summary.append(" [").append(packet.getHttpStatus()).append("]");
        }
        
        if (packet.getPacketLength() != null) {
            summary.append(" (").append(packet.getPacketLength()).append(" bytes)");
        }
        
        return summary.toString();
    }
}