package com.example.netcapture.dto;

import lombok.Data;

@Data
public class CaptureControlDto {
    
    private String action;
    
    private String networkInterface;
    
    private String filter;
    
    private Boolean promiscuous;
    
    private Integer maxPackets;
}