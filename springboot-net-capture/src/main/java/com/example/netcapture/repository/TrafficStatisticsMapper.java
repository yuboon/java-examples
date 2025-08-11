package com.example.netcapture.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.netcapture.entity.TrafficStatistics;

@Mapper
public interface TrafficStatisticsMapper extends BaseMapper<TrafficStatistics> {
    
    @Select("SELECT * FROM traffic_statistics WHERE statistics_time >= #{startTime} ORDER BY statistics_time DESC LIMIT #{limit}")
    List<TrafficStatistics> findRecentStatistics(@Param("startTime") LocalDateTime startTime, @Param("limit") int limit);
    
    @Select("SELECT FORMATDATETIME(statistics_time, 'HH:mm') as timeWindow, total_packets as totalPackets, total_bytes as totalBytes " +
            "FROM traffic_statistics WHERE statistics_time >= #{startTime} ORDER BY statistics_time DESC LIMIT 20")
    List<java.util.Map<String, Object>> getTrafficTrend(@Param("startTime") LocalDateTime startTime);
}