package com.example.netcapture.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "network.capture")
public class NetworkCaptureProperties {
    
    private String networkInterface = "";
    
    private String filter = "";
    
    private int bufferSize = 65536;
    
    private int timeout = 1000;
    
    private boolean promiscuous = false;
    
    private int maxPackets = 0;
    
    private boolean autoStart = false;
    
    private int dataRetentionHours = 24;
}