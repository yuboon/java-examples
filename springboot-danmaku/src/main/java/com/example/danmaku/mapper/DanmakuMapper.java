package com.example.danmaku.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.danmaku.model.Danmaku;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface DanmakuMapper extends BaseMapper<Danmaku> {
    
    /**
     * 根据视频ID查询所有弹幕，按时间排序
     */
    @Select("SELECT * FROM danmaku WHERE video_id = #{videoId} ORDER BY time ASC")
    List<Danmaku> findByVideoIdOrderByTimeAsc(@Param("videoId") String videoId);
    
    /**
     * 根据视频ID和时间范围查询弹幕
     */
    @Select("SELECT * FROM danmaku WHERE video_id = #{videoId} AND time BETWEEN #{startTime} AND #{endTime} ORDER BY time ASC")
    List<Danmaku> findByVideoIdAndTimeBetween(
            @Param("videoId") String videoId, 
            @Param("startTime") Double startTime, 
            @Param("endTime") Double endTime);
}