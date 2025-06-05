package com.example.livestream.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.livestream.entity.LiveRecording;
import com.example.livestream.entity.LiveRoom;
import com.example.livestream.mapper.LiveRecordingMapper;
import com.example.livestream.mapper.LiveRoomMapper;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.UploadObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@Slf4j
public class LiveRecordingService {
    
    @Autowired
    private LiveRoomMapper liveRoomMapper;
    
    @Autowired
    private LiveRecordingMapper recordingMapper;
    
    @Autowired
    private MinioClient minioClient;
    
    @Value("${minio.bucket}")
    private String minioBucket;
    
    @Value("${live.record.save-path}")
    private String recordSavePath;
    
    /**
     * 开始录制直播
     */
    @Transactional
    public LiveRecording startRecording(Long roomId) {
        LiveRoom liveRoom = liveRoomMapper.selectById(roomId);
        if (liveRoom == null || liveRoom.getStatus() != 1) {
            throw new IllegalArgumentException("直播间不存在或未开播");
        }
        
        // 创建录制记录
        LiveRecording recording = new LiveRecording();
        recording.setRoomId(roomId);
        recording.setFileName(liveRoom.getStreamKey() + "_" + System.currentTimeMillis() + ".mp4");
        recording.setStatus(0);  // 录制中
        recording.setStartTime(LocalDateTime.now());
        recording.setCreatedAt(LocalDateTime.now());
        recording.setUpdatedAt(LocalDateTime.now());
        
        recordingMapper.insert(recording);
        
        // 异步启动录制进程
        startRecordingProcess(liveRoom, recording);
        
        return recording;
    }
    
    /**
     * 停止录制直播
     */
    @Transactional
    public LiveRecording stopRecording(Long recordingId) {
        LiveRecording recording = recordingMapper.selectById(recordingId);
        if (recording == null || recording.getStatus() != 0) {
            throw new IllegalArgumentException("录制任务不存在或已结束");
        }
        
        // 更新录制状态
        recording.setStatus(1);  // 录制完成
        recording.setEndTime(LocalDateTime.now());
        recording.setUpdatedAt(LocalDateTime.now());
        
        recordingMapper.updateById(recording);
        
        // 异步停止录制进程并上传文件
        stopRecordingProcess(recording);
        
        return recording;
    }
    
    /**
     * 获取直播回放列表
     */
    public List<LiveRecording> getRecordings(Long roomId, int page, int size) {
        Page<LiveRecording> pageParam = new Page<>(page, size);
        QueryWrapper<LiveRecording> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("room_id", roomId)
                   .eq("status", 3)  // 可用状态
                   .orderByDesc("start_time");
        
        return recordingMapper.selectPage(pageParam, queryWrapper).getRecords();
    }
    
    /**
     * 启动录制进程
     */
    private void startRecordingProcess(LiveRoom liveRoom, LiveRecording recording) {
        // 使用线程池异步执行
        CompletableFuture.runAsync(() -> {
            try {
                File saveDir = new File(recordSavePath);
                if (!saveDir.exists()) {
                    saveDir.mkdirs();
                }
                
                String outputPath = recordSavePath + "/" + recording.getFileName();
                String inputUrl = liveRoom.getHlsUrl();
                
                // 使用FFmpeg录制
                ProcessBuilder pb = new ProcessBuilder(
                    "ffmpeg", 
                    "-i", inputUrl,
                    "-c:v", "copy",
                    "-c:a", "aac",
                    "-strict", "-2",
                    outputPath
                );
                
                Process process = pb.start();
                
                // 保存进程ID，以便后续停止
                String processKey = "live:recording:process:" + recording.getId();
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    process.destroy();
                }));
                
                log.info("开始录制直播, roomId={}, recordingId={}", liveRoom.getId(), recording.getId());
                
                // 等待进程结束
                int exitCode = process.waitFor();
                
                log.info("录制进程结束, roomId={}, recordingId={}, exitCode={}", 
                        liveRoom.getId(), recording.getId(), exitCode);
                
                // 如果是正常结束，则更新状态并上传文件
                if (exitCode == 0) {
                    uploadRecording(recording, new File(outputPath));
                }
                
            } catch (Exception e) {
                log.error("录制直播异常", e);
                
                // 更新录制状态为失败
                LiveRecording failedRecording = new LiveRecording();
                failedRecording.setId(recording.getId());
                failedRecording.setStatus(4);  // 失败状态
                failedRecording.setUpdatedAt(LocalDateTime.now());
                
                recordingMapper.updateById(failedRecording);
            }
        });
    }
    
    /**
     * 停止录制进程
     */
    private void stopRecordingProcess(LiveRecording recording) {
        // 这里可以实现停止特定的FFmpeg进程
        // 在实际实现中，需要保存进程ID并通过操作系统命令停止进程
        log.info("手动停止录制, recordingId={}", recording.getId());
    }
    
    /**
     * 上传录制文件到MinIO
     */
    private void uploadRecording(LiveRecording recording, File file) {
        try {
            // 设置状态为处理中
            LiveRecording processingRecording = new LiveRecording();
            processingRecording.setId(recording.getId());
            processingRecording.setStatus(2);  // 处理中
            processingRecording.setUpdatedAt(LocalDateTime.now());
            recordingMapper.updateById(processingRecording);
            
            // 获取文件元数据
            long fileSize = file.length();
            
            // 使用FFmpeg获取视频时长
            String[] cmd = {
                "ffprobe", 
                "-v", "error", 
                "-show_entries", "format=duration", 
                "-of", "default=noprint_wrappers=1:nokey=1", 
                file.getAbsolutePath()
            };
            
            Process process = Runtime.getRuntime().exec(cmd);
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String durationStr = reader.readLine();
            int duration = (int) Float.parseFloat(durationStr);
            
            // 上传到MinIO
            String objectName = "recordings/" + recording.getFileName();
            minioClient.uploadObject(
                UploadObjectArgs.builder()
                    .bucket(minioBucket)
                    .object(objectName)
                    .filename(file.getAbsolutePath())
                    .contentType("video/mp4")
                    .build()
            );
            
            // 构建访问URL
            String fileUrl = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .bucket(minioBucket)
                    .object(objectName)
                    .method(Method.GET)
                    .build()
            );
            
            // 更新录制记录
            LiveRecording updatedRecording = new LiveRecording();
            updatedRecording.setId(recording.getId());
            updatedRecording.setFileUrl(fileUrl);
            updatedRecording.setFileSize(fileSize);
            updatedRecording.setDuration(duration);
            updatedRecording.setStatus(3);  // 可用状态
            updatedRecording.setUpdatedAt(LocalDateTime.now());
            
            recordingMapper.updateById(updatedRecording);
            
            log.info("录制文件上传完成, recordingId={}, fileSize={}, duration={}s", 
                    recording.getId(), fileSize, duration);
            
            // 删除本地文件
            file.delete();
            
        } catch (Exception e) {
            log.error("上传录制文件异常", e);
            
            // 更新录制状态为失败
            LiveRecording failedRecording = new LiveRecording();
            failedRecording.setId(recording.getId());
            failedRecording.setStatus(4);  // 失败状态
            failedRecording.setUpdatedAt(LocalDateTime.now());
            
            recordingMapper.updateById(failedRecording);
        }
    }
}