package com.example.webssh.controller;

import com.example.webssh.entity.ServerConfig;
import com.example.webssh.service.FileTransferService;
import com.example.webssh.service.ServerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/files")
public class FileTransferController {

    @Autowired
    private FileTransferService fileTransferService;

    @Autowired
    private ServerService serverService;

    /**
     * 上传文件到服务器
     */
    @PostMapping("/upload/{serverId}")
    public ResponseEntity<Map<String, Object>> uploadFile(
            @PathVariable Long serverId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("remotePath") String remotePath) {
        try {
            Optional<ServerConfig> serverOpt = serverService.getServerById(serverId);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "服务器不存在"));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "文件不能为空"));
            }

            fileTransferService.uploadFile(serverOpt.get(), file, remotePath);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "文件上传成功",
                    "filename", file.getOriginalFilename(),
                    "size", file.getSize()
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "上传失败: " + e.getMessage()));
        }
    }

    /**
     * 批量上传文件
     */
    @PostMapping("/upload-batch/{serverId}")
    public ResponseEntity<Map<String, Object>> uploadFiles(
            @PathVariable Long serverId,
            @RequestParam("files") MultipartFile[] files,
            @RequestParam("remotePath") String remotePath) {
        try {
            Optional<ServerConfig> serverOpt = serverService.getServerById(serverId);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "服务器不存在"));
            }

            if (files == null || files.length == 0) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "请选择要上传的文件"));
            }

            fileTransferService.uploadFiles(serverOpt.get(), files, remotePath);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "批量上传成功",
                    "count", files.length
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "批量上传失败: " + e.getMessage()));
        }
    }

    /**
     * 从服务器下载文件
     */
    @GetMapping("/download/{serverId}")
    public ResponseEntity<byte[]> downloadFile(
            @PathVariable Long serverId,
            @RequestParam("remoteFilePath") String remoteFilePath) {
        try {
            Optional<ServerConfig> serverOpt = serverService.getServerById(serverId);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.badRequest().build();
            }

            byte[] fileContent = fileTransferService.downloadFile(serverOpt.get(), remoteFilePath);

            // 从路径中提取文件名
            String filename = remoteFilePath.substring(remoteFilePath.lastIndexOf('/') + 1);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .body(fileContent);

        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }

    /**
     * 列出远程目录内容
     */
    @GetMapping("/list/{serverId}")
    public ResponseEntity<Map<String, Object>> listDirectory(
            @PathVariable Long serverId,
            @RequestParam("remotePath") String remotePath) {
        try {
            Optional<ServerConfig> serverOpt = serverService.getServerById(serverId);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "服务器不存在"));
            }

            List<FileTransferService.FileInfo> files = 
                    fileTransferService.listDirectory(serverOpt.get(), remotePath);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "files", files,
                    "path", remotePath
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "获取目录列表失败: " + e.getMessage()));
        }
    }

    /**
     * 创建远程目录
     */
    @PostMapping("/mkdir/{serverId}")
    public ResponseEntity<Map<String, Object>> createDirectory(
            @PathVariable Long serverId,
            @RequestBody Map<String, String> request) {
        try {
            Optional<ServerConfig> serverOpt = serverService.getServerById(serverId);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "服务器不存在"));
            }

            String remotePath = request.get("remotePath");
            if (remotePath == null || remotePath.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "目录路径不能为空"));
            }

            fileTransferService.createRemoteDirectory(serverOpt.get(), remotePath);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "目录创建成功",
                    "path", remotePath
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "创建目录失败: " + e.getMessage()));
        }
    }

    /**
     * 删除远程文件或目录
     */
    @DeleteMapping("/delete/{serverId}")
    public ResponseEntity<Map<String, Object>> deleteFile(
            @PathVariable Long serverId,
            @RequestParam("remotePath") String remotePath,
            @RequestParam(value = "isDirectory", defaultValue = "false") boolean isDirectory) {
        try {
            Optional<ServerConfig> serverOpt = serverService.getServerById(serverId);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "服务器不存在"));
            }

            fileTransferService.deleteRemoteFile(serverOpt.get(), remotePath, isDirectory);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", (isDirectory ? "目录" : "文件") + "删除成功",
                    "path", remotePath
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "删除失败: " + e.getMessage()));
        }
    }

    /**
     * 重命名远程文件
     */
    @PostMapping("/rename/{serverId}")
    public ResponseEntity<Map<String, Object>> renameFile(
            @PathVariable Long serverId,
            @RequestBody Map<String, String> request) {
        try {
            Optional<ServerConfig> serverOpt = serverService.getServerById(serverId);
            if (!serverOpt.isPresent()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "服务器不存在"));
            }

            String oldPath = request.get("oldPath");
            String newPath = request.get("newPath");

            if (oldPath == null || newPath == null || oldPath.trim().isEmpty() || newPath.trim().isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("success", false, "message", "路径不能为空"));
            }

            fileTransferService.renameRemoteFile(serverOpt.get(), oldPath, newPath);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "重命名成功",
                    "oldPath", oldPath,
                    "newPath", newPath
            ));

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("success", false, "message", "重命名失败: " + e.getMessage()));
        }
    }
}