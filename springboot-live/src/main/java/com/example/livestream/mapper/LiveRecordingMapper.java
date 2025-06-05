package com.example.livestream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.livestream.entity.LiveRecording;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LiveRecordingMapper extends BaseMapper<LiveRecording> {
    // 基础CRUD方法由BaseMapper提供
}