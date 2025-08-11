package com.example.netcapture.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;

import lombok.Data;

@Data
@TableName("traffic_statistics")
public class TrafficStatistics {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("statistics_time")
    private LocalDateTime statisticsTime;
    
    @TableField("time_window")
    private String timeWindow;
    
    @TableField("total_packets")
    private Long totalPackets;
    
    @TableField("total_bytes")
    private Long totalBytes;
    
    @TableField("http_packets")
    private Long httpPackets;
    
    @TableField("tcp_packets")
    private Long tcpPackets;
    
    @TableField("udp_packets")
    private Long udpPackets;
    
    @TableField("icmp_packets")
    private Long icmpPackets;
    
    @TableField("top_source_ip")
    private String topSourceIp;
    
    @TableField("top_destination_ip")
    private String topDestinationIp;
    
    @TableField("top_source_port")
    private Integer topSourcePort;
    
    @TableField("top_destination_port")
    private Integer topDestinationPort;
    
    @TableField("average_packet_size")
    private Double averagePacketSize;
}