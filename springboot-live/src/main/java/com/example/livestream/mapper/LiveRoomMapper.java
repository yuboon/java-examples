package com.example.livestream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.livestream.entity.LiveRecording;
import com.example.livestream.entity.LiveRoom;
import com.example.livestream.entity.LiveStream;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface LiveRoomMapper extends BaseMapper<LiveRoom> {
    // 自定义查询方法
    @Select("SELECT * FROM live_room WHERE status = 1 ORDER BY view_count DESC LIMIT #{limit}")
    List<LiveRoom> findHotLiveRooms(@Param("limit") int limit);
}


