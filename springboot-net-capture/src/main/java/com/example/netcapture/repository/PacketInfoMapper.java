package com.example.netcapture.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.netcapture.entity.PacketInfo;

@Mapper
public interface PacketInfoMapper extends BaseMapper<PacketInfo> {
    
    @Select("SELECT * FROM packet_info WHERE capture_time >= #{startTime} AND capture_time <= #{endTime} ORDER BY capture_time DESC LIMIT #{limit}")
    List<PacketInfo> findByTimeRange(@Param("startTime") LocalDateTime startTime, 
                                   @Param("endTime") LocalDateTime endTime, 
                                   @Param("limit") int limit);
    
    @Select("SELECT protocol as protocol, COUNT(*) as count FROM packet_info WHERE capture_time >= #{startTime} AND protocol IS NOT NULL GROUP BY protocol")
    List<java.util.Map<String, Object>> getProtocolStatistics(@Param("startTime") LocalDateTime startTime);
    
    @Select("SELECT source_ip as source_ip, COUNT(*) as count FROM packet_info WHERE capture_time >= #{startTime} AND source_ip IS NOT NULL GROUP BY source_ip ORDER BY count DESC LIMIT 10")
    List<java.util.Map<String, Object>> getTopSourceIps(@Param("startTime") LocalDateTime startTime);
    
    @Select("SELECT destination_ip as destination_ip, COUNT(*) as count FROM packet_info WHERE capture_time >= #{startTime} AND destination_ip IS NOT NULL GROUP BY destination_ip ORDER BY count DESC LIMIT 10")
    List<java.util.Map<String, Object>> getTopDestinationIps(@Param("startTime") LocalDateTime startTime);
}