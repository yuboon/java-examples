package com.example.netcapture.service;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.example.netcapture.entity.PacketInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProtocolAnalyzer {
    
    private static final Pattern HTTP_REQUEST_PATTERN = Pattern.compile(
        "^(GET|POST|PUT|DELETE|HEAD|OPTIONS|PATCH)\\s+(\\S+)\\s+HTTP/([0-9\\.]+)"
    );
    
    private static final Pattern HTTP_RESPONSE_PATTERN = Pattern.compile(
        "^HTTP/([0-9\\.]+)\\s+(\\d{3})\\s*(.*)"
    );
    
    private static final Pattern HTTP_HEADER_PATTERN = Pattern.compile(
        "^([^:]+):\\s*(.*)$"
    );
    
    public void analyzeHttpPacket(PacketInfo packetInfo) {
        if (packetInfo.getPayload() == null || packetInfo.getPayload().isEmpty()) {
            return;
        }
        
        String payload = packetInfo.getPayload();
        
        if (isHttpRequest(payload)) {
            parseHttpRequest(packetInfo, payload);
        } else if (isHttpResponse(payload)) {
            parseHttpResponse(packetInfo, payload);
        }
    }
    
    private boolean isHttpRequest(String payload) {
        return HTTP_REQUEST_PATTERN.matcher(payload).find();
    }
    
    private boolean isHttpResponse(String payload) {
        return HTTP_RESPONSE_PATTERN.matcher(payload).find();
    }
    
    private void parseHttpRequest(PacketInfo packetInfo, String payload) {
        try {
            String[] lines = payload.split("\\r?\\n");
            if (lines.length == 0) return;
            
            Matcher requestMatcher = HTTP_REQUEST_PATTERN.matcher(lines[0]);
            if (requestMatcher.find()) {
                packetInfo.setHttpMethod(requestMatcher.group(1));
                packetInfo.setHttpUrl(requestMatcher.group(2));
            }
            
            Map<String, String> headers = parseHeaders(lines);
            packetInfo.setHttpHeaders(formatHeaders(headers));
            
            String body = extractHttpBody(lines);
            if (body != null && !body.isEmpty()) {
                packetInfo.setHttpBody(body.length() > 500 ? body.substring(0, 500) + "..." : body);
            }
            
        } catch (Exception e) {
            log.debug("Error parsing HTTP request", e);
        }
    }
    
    private void parseHttpResponse(PacketInfo packetInfo, String payload) {
        try {
            String[] lines = payload.split("\\r?\\n");
            if (lines.length == 0) return;
            
            Matcher responseMatcher = HTTP_RESPONSE_PATTERN.matcher(lines[0]);
            if (responseMatcher.find()) {
                packetInfo.setHttpStatus(Integer.parseInt(responseMatcher.group(2)));
            }
            
            Map<String, String> headers = parseHeaders(lines);
            packetInfo.setHttpHeaders(formatHeaders(headers));
            
            String body = extractHttpBody(lines);
            if (body != null && !body.isEmpty()) {
                packetInfo.setHttpBody(body.length() > 500 ? body.substring(0, 500) + "..." : body);
            }
            
        } catch (Exception e) {
            log.debug("Error parsing HTTP response", e);
        }
    }
    
    private Map<String, String> parseHeaders(String[] lines) {
        Map<String, String> headers = new HashMap<>();
        
        for (int i = 1; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.isEmpty()) {
                break;
            }
            
            Matcher headerMatcher = HTTP_HEADER_PATTERN.matcher(line);
            if (headerMatcher.find()) {
                headers.put(headerMatcher.group(1).trim(), headerMatcher.group(2).trim());
            }
        }
        
        return headers;
    }
    
    private String extractHttpBody(String[] lines) {
        StringBuilder body = new StringBuilder();
        boolean inBody = false;
        
        for (String line : lines) {
            if (inBody) {
                body.append(line).append("\\n");
            } else if (line.trim().isEmpty()) {
                inBody = true;
            }
        }
        
        return body.toString();
    }
    
    private String formatHeaders(Map<String, String> headers) {
        if (headers.isEmpty()) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        headers.forEach((key, value) -> 
            sb.append(key).append(": ").append(value).append("; ")
        );
        
        return sb.toString();
    }
    
    public void analyzeTcpPacket(PacketInfo packetInfo) {
        if (packetInfo.getSourcePort() == null || packetInfo.getDestinationPort() == null) {
            return;
        }
        
        int srcPort = packetInfo.getSourcePort();
        int dstPort = packetInfo.getDestinationPort();
        
        // 根据端口识别具体协议，优先级从高到低
        if (srcPort == 80 || dstPort == 80 || srcPort == 8080 || dstPort == 8080) {
            packetInfo.setProtocol("HTTP");
            analyzeHttpPacket(packetInfo);
        } else if (srcPort == 443 || dstPort == 443 || srcPort == 8443 || dstPort == 8443) {
            packetInfo.setProtocol("HTTPS");
            // HTTPS是加密的，无法解析HTTP内容，但可以尝试解析SNI
            analyzeHttpPacket(packetInfo);
        } else if (srcPort == 21 || dstPort == 21) {
            packetInfo.setProtocol("FTP");
        } else if (srcPort == 22 || dstPort == 22) {
            packetInfo.setProtocol("SSH");
        } else if (srcPort == 23 || dstPort == 23) {
            packetInfo.setProtocol("TELNET");
        } else if (srcPort == 25 || dstPort == 25) {
            packetInfo.setProtocol("SMTP");
        } else if (srcPort == 53 || dstPort == 53) {
            packetInfo.setProtocol("DNS");
        } else {
            // 如果没有匹配到特定协议，保持TCP标记
            // packetInfo.setProtocol("TCP") - 已经在parsePacket中设置
        }
    }
    
    public void analyzeUdpPacket(PacketInfo packetInfo) {
        if (packetInfo.getSourcePort() == null || packetInfo.getDestinationPort() == null) {
            return;
        }
        
        int srcPort = packetInfo.getSourcePort();
        int dstPort = packetInfo.getDestinationPort();
        
        if (srcPort == 53 || dstPort == 53) {
            packetInfo.setProtocol("DNS");
            analyzeDnsPacket(packetInfo);
        } else if (srcPort == 67 || dstPort == 67 || srcPort == 68 || dstPort == 68) {
            packetInfo.setProtocol("DHCP");
        } else if (srcPort == 123 || dstPort == 123) {
            packetInfo.setProtocol("NTP");
        } else if (srcPort == 161 || dstPort == 161) {
            packetInfo.setProtocol("SNMP");
        }
    }
    
    private void analyzeDnsPacket(PacketInfo packetInfo) {
        if (packetInfo.getPayload() == null || packetInfo.getPayload().length() < 12) {
            return;
        }
        
        try {
            byte[] payload = packetInfo.getPayload().getBytes(StandardCharsets.ISO_8859_1);
            
            if (payload.length >= 12) {
                int flags = (payload[2] & 0xFF) << 8 | (payload[3] & 0xFF);
                boolean isResponse = (flags & 0x8000) != 0;
                
                if (isResponse) {
                    packetInfo.setHttpMethod("DNS_RESPONSE");
                } else {
                    packetInfo.setHttpMethod("DNS_QUERY");
                }
            }
        } catch (Exception e) {
            log.debug("Error analyzing DNS packet", e);
        }
    }
}