package com.example.netcapture.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class PacketInfoDto {
    
    private Long id;
    
    private LocalDateTime captureTime;
    
    private String sourceIp;
    
    private String destinationIp;
    
    private Integer sourcePort;
    
    private Integer destinationPort;
    
    private String protocol;
    
    private Integer packetLength;
    
    private String httpMethod;
    
    private String httpUrl;
    
    private Integer httpStatus;
    
    private String tcpFlags;
    
    private String networkInterface;
    
    private String summary;
}