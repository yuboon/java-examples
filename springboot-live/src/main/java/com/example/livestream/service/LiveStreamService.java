package com.example.livestream.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.livestream.entity.LiveRoom;
import com.example.livestream.entity.LiveStream;
import com.example.livestream.mapper.LiveRoomMapper;
import com.example.livestream.mapper.LiveStreamMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
public class LiveStreamService {
    
    @Autowired
    private LiveRoomMapper liveRoomMapper;
    
    @Autowired
    private LiveStreamMapper liveStreamMapper;
    
    @Autowired
    private StringRedisTemplate redisTemplate;
    
    @Value("${live.srs.server-url}")
    private String srsServerUrl;
    
    @Value("${live.srs.api-url}")
    private String srsApiUrl;
    
    @Value("${live.srs.http-flv-url}")
    private String httpFlvUrl;
    
    @Value("${live.srs.hls-url}")
    private String hlsUrl;
    
    @Value("${live.push.key-check-enabled}")
    private boolean keyCheckEnabled;
    
    @Value("${live.push.auth-expire}")
    private long authExpire;
    
    @Value("${live.push.auth-key}")
    private String authKey;
    
    private RestTemplate restTemplate = new RestTemplate();
    
    /**
     * 创建直播间
     */
    @Transactional
    public LiveRoom createLiveRoom(LiveRoom liveRoom) {
        // 生成推流密钥
        String streamKey = generateStreamKey(liveRoom.getUserId());
        liveRoom.setStreamKey(streamKey);
        
        // 构建推流地址
        String pushUrl = buildPushUrl(streamKey);
        liveRoom.setStreamUrl(pushUrl);
        
        // 构建播放地址
        liveRoom.setHlsUrl(hlsUrl + "/" + streamKey +"/" + streamKey + ".m3u8");
        liveRoom.setFlvUrl(httpFlvUrl + "/" + streamKey + "/" + streamKey + ".flv");
        
        // 设置初始状态
        liveRoom.setStatus(0);
        liveRoom.setViewCount(0L);
        liveRoom.setLikeCount(0L);
        liveRoom.setCreatedAt(LocalDateTime.now());
        liveRoom.setUpdatedAt(LocalDateTime.now());
        
        // 保存到数据库
        liveRoomMapper.insert(liveRoom);
        
        return liveRoom;
    }
    
    /**
     * 生成推流密钥
     */
    private String generateStreamKey(Long userId) {
        // 生成基于用户ID和时间戳的唯一密钥
        String baseKey = userId + "_" + System.currentTimeMillis();
        return DigestUtils.md5DigestAsHex(baseKey.getBytes());
    }
    
    /**
     * 构建推流地址
     */
    private String buildPushUrl(String streamKey) {
        StringBuilder sb = new StringBuilder(srsServerUrl);
        sb.append("/").append(streamKey);
        
        // 如果启用了推流验证
        if (keyCheckEnabled) {
            long expireTimestamp = System.currentTimeMillis() / 1000 + authExpire;
            String authString = streamKey + "-" + expireTimestamp + "-" + authKey;
            String authToken = DigestUtils.md5DigestAsHex(authString.getBytes());
            
            sb.append("?auth_key=").append(authToken)
              .append("&expire=").append(expireTimestamp);
        }
        
        return sb.toString();
    }
    
    /**
     * 开始直播
     */
    @Transactional
    public LiveRoom startLiveStream(Long roomId) {
        LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
        if (liveRoom == null) {
            throw new IllegalArgumentException("直播间不存在");
        }
        
        // 更新直播间状态为直播中
        liveRoom.setStatus(1);
        liveRoom.setStartTime(LocalDateTime.now());
        liveRoomMapper.updateById(liveRoom);
        
        // 创建直播流记录
        LiveStream liveStream = new LiveStream();
        liveStream.setRoomId(roomId);
        liveStream.setStreamId(liveRoom.getStreamKey());
        liveStream.setProtocol("rtmp");
        liveStream.setStatus(1);
        liveStream.setCreatedAt(LocalDateTime.now());
        liveStream.setUpdatedAt(LocalDateTime.now());
        liveStreamMapper.insert(liveStream);
        
        // 更新Redis缓存中的活跃直播间
        redisTemplate.opsForSet().add("live:active_rooms", String.valueOf(roomId));
        
        return liveRoom;
    }
    
    /**
     * 结束直播
     */
    @Transactional
    public LiveRoom endLiveStream(Long roomId) {
        LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
        if (liveRoom == null || liveRoom.getStatus() != 1) {
            throw new IllegalArgumentException("直播间不存在或未开播");
        }
        
        // 更新直播间状态为已结束
        liveRoom.setStatus(2);
        liveRoom.setEndTime(LocalDateTime.now());
        liveRoomMapper.updateById(liveRoom);
        
        // 更新直播流状态
        QueryWrapper<LiveStream> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId).eq("status", 1);
        
        LiveStream liveStream = liveStreamMapper.selectOne(queryWrapper);
        if (liveStream != null) {
            liveStream.setStatus(2);
            liveStream.setUpdatedAt(LocalDateTime.now());
            liveStreamMapper.updateById(liveStream);
        }
        
        // 从Redis中移除活跃直播间
        redisTemplate.opsForSet().remove("live:active_rooms", String.valueOf(roomId));
        
        return liveRoom;
    }
    
    /**
     * 获取当前活跃的直播间列表
     */
    public List<LiveRoom> getActiveLiveRooms(int page, int size) {
        Page<LiveRoom> pageParam = new Page<>(page, size);
        QueryWrapper<LiveRoom> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("status", 1).orderByDesc("view_count");
        
        return liveRoomMapper.selectPage(pageParam, queryWrapper).getRecords();
    }
    
    /**
     * 获取热门直播间
     */
    public List<LiveRoom> getHotLiveRooms(int limit) {
        return liveRoomMapper.findHotLiveRooms(limit);
    }
    
    /**
     * 增加直播间观看人数
     */
    public void incrementViewCount(Long roomId) {
        // 使用Redis进行计数
        String key = "live:room:" + roomId + ":view_count";
        redisTemplate.opsForValue().increment(key);
        
        // 定期同步到数据库
        if (Math.random() < 0.1) {  // 10%概率同步，减少数据库压力
            String countStr = redisTemplate.opsForValue().get(key);
            if (countStr != null) {
                long count = Long.parseLong(countStr);
                
                LiveRoom room = new LiveRoom();
                room.setId(roomId);
                room.setViewCount(count);
                liveRoomMapper.updateById(room);
            }
        }
    }
    
    /**
     * 校验推流密钥
     */
    public boolean validateStreamKey(String streamKey, String token, String expire) {
        if (!keyCheckEnabled) {
            return true;
        }
        
        try {
            long expireTimestamp = Long.parseLong(expire);
            long currentTime = System.currentTimeMillis() / 1000;
            
            // 检查是否过期
            if (currentTime > expireTimestamp) {
                return false;
            }
            
            // 验证token
            String authString = streamKey + "-" + expire + "-" + authKey;
            String calculatedToken = DigestUtils.md5DigestAsHex(authString.getBytes());
            
            return calculatedToken.equals(token);
            
        } catch (Exception e) {
            log.error("验证推流密钥异常", e);
            return false;
        }
    }
    
    /**
     * 处理SRS回调 - 流发布
     */
    public void handleStreamPublish(String app, String stream) {
        try {
            // 查找对应的直播间
            QueryWrapper<LiveRoom> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("stream_key", stream);
            
            LiveRoom liveRoom = liveRoomMapper.selectOne(queryWrapper);
            if (liveRoom != null && liveRoom.getStatus() == 0) {
                // 更新直播间状态
                startLiveStream(liveRoom.getId());
                
                log.info("直播流发布成功: app={}, stream={}, roomId={}", app, stream, liveRoom.getId());
            }
        } catch (Exception e) {
            log.error("处理流发布回调异常", e);
        }
    }
    
    /**
     * 处理SRS回调 - 流关闭
     */
    public void handleStreamClose(String app, String stream) {
        try {
            // 查找对应的直播间
            QueryWrapper<LiveRoom> queryWrapper = new QueryWrapper<>();
            queryWrapper.eq("stream_key", stream);
            
            LiveRoom liveRoom = liveRoomMapper.selectOne(queryWrapper);
            if (liveRoom != null && liveRoom.getStatus() == 1) {
                // 更新直播间状态
                endLiveStream(liveRoom.getId());
                
                log.info("直播流关闭: app={}, stream={}, roomId={}", app, stream, liveRoom.getId());
            }
        } catch (Exception e) {
            log.error("处理流关闭回调异常", e);
        }
    }
    
    /**
     * 获取SRS服务器信息
     */
    public Map<String, Object> getSrsServerInfo() {
        try {
            String url = srsApiUrl + "/v1/summaries";
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("获取SRS服务器信息异常", e);
            return Collections.emptyMap();
        }
    }
}