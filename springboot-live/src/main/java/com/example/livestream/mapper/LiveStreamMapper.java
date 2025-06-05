package com.example.livestream.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.livestream.entity.LiveStream;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface LiveStreamMapper extends BaseMapper<LiveStream> {
    // 基础CRUD方法由BaseMapper提供
}