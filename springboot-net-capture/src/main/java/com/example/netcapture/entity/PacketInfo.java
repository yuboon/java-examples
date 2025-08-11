package com.example.netcapture.entity;

import java.time.LocalDateTime;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.TableField;

import lombok.Data;

@Data
@TableName("packet_info")
public class PacketInfo {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    @TableField("capture_time")
    private LocalDateTime captureTime;
    
    @TableField("source_ip")
    private String sourceIp;
    
    @TableField("destination_ip")
    private String destinationIp;
    
    @TableField("source_port")
    private Integer sourcePort;
    
    @TableField("destination_port")
    private Integer destinationPort;
    
    private String protocol;
    
    @TableField("packet_length")
    private Integer packetLength;
    
    private String payload;
    
    @TableField("http_method")
    private String httpMethod;
    
    @TableField("http_url")
    private String httpUrl;
    
    @TableField("http_headers")
    private String httpHeaders;
    
    @TableField("http_body")
    private String httpBody;
    
    @TableField("http_status")
    private Integer httpStatus;
    
    @TableField("tcp_seq_number")
    private Long tcpSeqNumber;
    
    @TableField("tcp_ack_number")
    private Long tcpAckNumber;
    
    @TableField("tcp_flags")
    private String tcpFlags;
    
    @TableField("network_interface")
    private String networkInterface;
}