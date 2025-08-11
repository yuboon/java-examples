package com.example.netcapture.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.netcapture.entity.PacketInfo;
import com.example.netcapture.entity.TrafficStatistics;
import com.example.netcapture.repository.PacketInfoMapper;
import com.example.netcapture.repository.TrafficStatisticsMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class TrafficStatisticsService {
    
    private final PacketInfoMapper packetInfoMapper;
    private final TrafficStatisticsMapper trafficStatisticsMapper;
    
    private final AtomicLong totalPackets = new AtomicLong(0);
    private final AtomicLong totalBytes = new AtomicLong(0);
    private final AtomicLong httpPackets = new AtomicLong(0);
    private final AtomicLong tcpPackets = new AtomicLong(0);
    private final AtomicLong udpPackets = new AtomicLong(0);
    private final AtomicLong icmpPackets = new AtomicLong(0);
    
    private final Map<String, AtomicLong> sourceIpCounts = new ConcurrentHashMap<>();
    private final Map<String, AtomicLong> destIpCounts = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicLong> sourcePortCounts = new ConcurrentHashMap<>();
    private final Map<Integer, AtomicLong> destPortCounts = new ConcurrentHashMap<>();
    
    public void updateStatistics(PacketInfo packetInfo) {
        // 基本统计更新
        totalPackets.incrementAndGet();
        totalBytes.addAndGet(packetInfo.getPacketLength() != null ? packetInfo.getPacketLength() : 0);
        
        // 协议统计更新
        String protocol = packetInfo.getProtocol();
        if ("TCP".equals(protocol)) {
            tcpPackets.incrementAndGet();
        } else if ("UDP".equals(protocol)) {
            udpPackets.incrementAndGet();
        } else if ("ICMP".equals(protocol)) {
            icmpPackets.incrementAndGet();
        }
        
        // HTTP/HTTPS 协议统计（包括通过端口识别的和通过内容识别的）
        if ("HTTP".equals(protocol) || "HTTPS".equals(protocol) ||
            (packetInfo.getHttpMethod() != null || packetInfo.getHttpStatus() != null)) {
            httpPackets.incrementAndGet();
        }
        
        // IP和端口统计更新（检查null值）
        if (packetInfo.getSourceIp() != null && !packetInfo.getSourceIp().trim().isEmpty()) {
            sourceIpCounts.computeIfAbsent(packetInfo.getSourceIp(), k -> new AtomicLong(0)).incrementAndGet();
        }
        if (packetInfo.getDestinationIp() != null && !packetInfo.getDestinationIp().trim().isEmpty()) {
            destIpCounts.computeIfAbsent(packetInfo.getDestinationIp(), k -> new AtomicLong(0)).incrementAndGet();
        }
        if (packetInfo.getSourcePort() != null) {
            sourcePortCounts.computeIfAbsent(packetInfo.getSourcePort(), k -> new AtomicLong(0)).incrementAndGet();
        }
        if (packetInfo.getDestinationPort() != null) {
            destPortCounts.computeIfAbsent(packetInfo.getDestinationPort(), k -> new AtomicLong(0)).incrementAndGet();
        }
    }
    
    @Scheduled(fixedRate = 60000) // 每分钟执行一次
    public void generateStatistics() {
        try {
            TrafficStatistics statistics = new TrafficStatistics();
            statistics.setStatisticsTime(LocalDateTime.now());
            statistics.setTimeWindow("1min");
            statistics.setTotalPackets(totalPackets.get());
            statistics.setTotalBytes(totalBytes.get());
            statistics.setHttpPackets(httpPackets.get());
            statistics.setTcpPackets(tcpPackets.get());
            statistics.setUdpPackets(udpPackets.get());
            statistics.setIcmpPackets(icmpPackets.get());
            
            // 处理可能为null的值
            String topSourceIp = getTopEntry(sourceIpCounts);
            String topDestIp = getTopEntry(destIpCounts);
            Integer topSourcePort = getTopEntryInt(sourcePortCounts);
            Integer topDestPort = getTopEntryInt(destPortCounts);
            
            statistics.setTopSourceIp(topSourceIp != null ? topSourceIp : "");
            statistics.setTopDestinationIp(topDestIp != null ? topDestIp : "");
            statistics.setTopSourcePort(topSourcePort != null ? topSourcePort : 0);
            statistics.setTopDestinationPort(topDestPort != null ? topDestPort : 0);
            
            double avgPacketSize = totalPackets.get() > 0 ? 
                (double) totalBytes.get() / totalPackets.get() : 0.0;
            statistics.setAveragePacketSize(avgPacketSize);
            
            trafficStatisticsMapper.insert(statistics);
            
            resetCounters();
            
            log.info("Traffic statistics generated: {} packets, {} bytes", 
                     totalPackets.get(), totalBytes.get());
            
        } catch (Exception e) {
            log.error("Error generating traffic statistics", e);
        }
    }
    
    @Scheduled(cron = "0 0 * * * ?") // 每小时执行一次
    public void cleanupOldData() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusHours(24);
            
            QueryWrapper<PacketInfo> packetWrapper = new QueryWrapper<>();
            packetWrapper.lt("capture_time", cutoffTime);
            packetInfoMapper.delete(packetWrapper);
            
            QueryWrapper<TrafficStatistics> statsWrapper = new QueryWrapper<>();
            statsWrapper.lt("statistics_time", cutoffTime);
            trafficStatisticsMapper.delete(statsWrapper);
            
            log.info("Cleaned up data older than {}", cutoffTime);
            
        } catch (Exception e) {
            log.error("Error cleaning up old data", e);
        }
    }
    
    public Map<String, Object> getCurrentStatistics() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        stats.put("totalPackets", totalPackets.get());
        stats.put("totalBytes", totalBytes.get());
        stats.put("httpPackets", httpPackets.get());
        stats.put("tcpPackets", tcpPackets.get());
        stats.put("udpPackets", udpPackets.get());
        stats.put("icmpPackets", icmpPackets.get());
        
        // 处理可能为null的值
        String topSourceIp = getTopEntry(sourceIpCounts);
        stats.put("topSourceIp", topSourceIp != null ? topSourceIp : "");
        
        String topDestIp = getTopEntry(destIpCounts);
        stats.put("topDestinationIp", topDestIp != null ? topDestIp : "");
        
        Integer topSourcePort = getTopEntryInt(sourcePortCounts);
        stats.put("topSourcePort", topSourcePort != null ? topSourcePort : 0);
        
        Integer topDestPort = getTopEntryInt(destPortCounts);
        stats.put("topDestinationPort", topDestPort != null ? topDestPort : 0);
        
        double avgPacketSize = totalPackets.get() > 0 ? 
            (double) totalBytes.get() / totalPackets.get() : 0.0;
        stats.put("averagePacketSize", avgPacketSize);
        
        return stats;
    }
    
    public List<TrafficStatistics> getRecentStatistics(int hours, int limit) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        return trafficStatisticsMapper.findRecentStatistics(startTime, limit);
    }
    
    public List<java.util.Map<String, Object>> getTrafficTrend(int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<java.util.Map<String, Object>> rawData = trafficStatisticsMapper.getTrafficTrend(startTime);
        
        // 规范化字段名
        return rawData.stream()
            .map(item -> {
                java.util.Map<String, Object> normalized = new java.util.HashMap<>();
                normalized.put("timeWindow", item.get("TIMEWINDOW") != null ? item.get("TIMEWINDOW") : item.get("timeWindow"));
                normalized.put("totalPackets", item.get("TOTALPACKETS") != null ? item.get("TOTALPACKETS") : item.get("totalPackets"));
                normalized.put("totalBytes", item.get("TOTALBYTES") != null ? item.get("TOTALBYTES") : item.get("totalBytes"));
                return normalized;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    public List<java.util.Map<String, Object>> getProtocolStatistics(int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<java.util.Map<String, Object>> rawData = packetInfoMapper.getProtocolStatistics(startTime);
        
        // 规范化字段名为小写
        return rawData.stream()
            .map(item -> {
                java.util.Map<String, Object> normalized = new java.util.HashMap<>();
                normalized.put("protocol", item.get("PROTOCOL") != null ? item.get("PROTOCOL") : item.get("protocol"));
                normalized.put("count", item.get("COUNT") != null ? item.get("COUNT") : item.get("count"));
                return normalized;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    public List<java.util.Map<String, Object>> getTopSourceIps(int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<java.util.Map<String, Object>> rawData = packetInfoMapper.getTopSourceIps(startTime);
        
        // 规范化字段名
        return rawData.stream()
            .map(item -> {
                java.util.Map<String, Object> normalized = new java.util.HashMap<>();
                normalized.put("source_ip", item.get("SOURCE_IP") != null ? item.get("SOURCE_IP") : item.get("source_ip"));
                normalized.put("count", item.get("COUNT") != null ? item.get("COUNT") : item.get("count"));
                return normalized;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    public List<java.util.Map<String, Object>> getTopDestinationIps(int hours) {
        LocalDateTime startTime = LocalDateTime.now().minusHours(hours);
        List<java.util.Map<String, Object>> rawData = packetInfoMapper.getTopDestinationIps(startTime);
        
        // 规范化字段名
        return rawData.stream()
            .map(item -> {
                java.util.Map<String, Object> normalized = new java.util.HashMap<>();
                normalized.put("destination_ip", item.get("DESTINATION_IP") != null ? item.get("DESTINATION_IP") : item.get("destination_ip"));
                normalized.put("count", item.get("COUNT") != null ? item.get("COUNT") : item.get("count"));
                return normalized;
            })
            .collect(java.util.stream.Collectors.toList());
    }
    
    private String getTopEntry(Map<String, AtomicLong> map) {
        return map.entrySet().stream()
            .max(Map.Entry.comparingByValue((a, b) -> Long.compare(a.get(), b.get())))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    private Integer getTopEntryInt(Map<Integer, AtomicLong> map) {
        return map.entrySet().stream()
            .max(Map.Entry.comparingByValue((a, b) -> Long.compare(a.get(), b.get())))
            .map(Map.Entry::getKey)
            .orElse(null);
    }
    
    private void resetCounters() {
        totalPackets.set(0);
        totalBytes.set(0);
        httpPackets.set(0);
        tcpPackets.set(0);
        udpPackets.set(0);
        icmpPackets.set(0);
        sourceIpCounts.clear();
        destIpCounts.clear();
        sourcePortCounts.clear();
        destPortCounts.clear();
    }
}