package com.example.webssh.service;

import com.example.webssh.entity.ServerConfig;
import com.jcraft.jsch.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Vector;

@Service
@Slf4j
public class FileTransferService {

    /**
     * 上传文件到远程服务器
     */
    public void uploadFile(ServerConfig server, MultipartFile file, String remotePath) throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = createSession(server);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            // 确保远程目录存在
            createRemoteDirectory(sftpChannel, remotePath);

            // 上传文件
            String remoteFilePath = remotePath + "/" + file.getOriginalFilename();
            try (InputStream inputStream = file.getInputStream()) {
                sftpChannel.put(inputStream, remoteFilePath);
            }

            log.info("文件上传成功: {} -> {}", file.getOriginalFilename(), remoteFilePath);

        } finally {
            closeConnections(sftpChannel, session);
        }
    }

    /**
     * 从远程服务器下载文件
     */
    public byte[] downloadFile(ServerConfig server, String remoteFilePath) throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = createSession(server);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                 InputStream inputStream = sftpChannel.get(remoteFilePath)) {
                
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                log.info("文件下载成功: {}", remoteFilePath);
                return outputStream.toByteArray();
            }

        } finally {
            closeConnections(sftpChannel, session);
        }
    }

    /**
     * 列出远程目录内容
     */
    @SuppressWarnings("unchecked")
    public List<FileInfo> listDirectory(ServerConfig server, String remotePath) throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;
        List<FileInfo> files = new ArrayList<>();

        try {
            session = createSession(server);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            Vector<ChannelSftp.LsEntry> entries = sftpChannel.ls(remotePath);
            
            for (ChannelSftp.LsEntry entry : entries) {
                String filename = entry.getFilename();
                if (!filename.equals(".") && !filename.equals("..")) {
                    SftpATTRS attrs = entry.getAttrs();
                    files.add(new FileInfo(
                        filename,
                        attrs.isDir(),
                        attrs.getSize(),
                        attrs.getMTime() * 1000L, // Convert to milliseconds
                        getPermissionString(attrs.getPermissions())
                    ));
                }
            }

            log.info("目录列表获取成功: {}, 文件数: {}", remotePath, files.size());
            return files;

        } finally {
            closeConnections(sftpChannel, session);
        }
    }

    /**
     * 创建远程目录
     */
    public void createRemoteDirectory(ServerConfig server, String remotePath) throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = createSession(server);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            createRemoteDirectory(sftpChannel, remotePath);
            log.info("远程目录创建成功: {}", remotePath);

        } finally {
            closeConnections(sftpChannel, session);
        }
    }

    /**
     * 删除远程文件或目录
     */
    public void deleteRemoteFile(ServerConfig server, String remotePath, boolean isDirectory) throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = createSession(server);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            if (isDirectory) {
                sftpChannel.rmdir(remotePath);
            } else {
                sftpChannel.rm(remotePath);
            }

            log.info("远程文件删除成功: {}", remotePath);

        } finally {
            closeConnections(sftpChannel, session);
        }
    }

    /**
     * 重命名远程文件
     */
    public void renameRemoteFile(ServerConfig server, String oldPath, String newPath) throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = createSession(server);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            sftpChannel.rename(oldPath, newPath);
            log.info("文件重命名成功: {} -> {}", oldPath, newPath);

        } finally {
            closeConnections(sftpChannel, session);
        }
    }

    /**
     * 批量上传文件
     */
    public void uploadFiles(ServerConfig server, MultipartFile[] files, String remotePath) throws Exception {
        Session session = null;
        ChannelSftp sftpChannel = null;

        try {
            session = createSession(server);
            sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();

            // 确保远程目录存在
            createRemoteDirectory(sftpChannel, remotePath);

            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String remoteFilePath = remotePath + "/" + file.getOriginalFilename();
                    try (InputStream inputStream = file.getInputStream()) {
                        sftpChannel.put(inputStream, remoteFilePath);
                        log.info("文件上传成功: {}", file.getOriginalFilename());
                    }
                }
            }

            log.info("批量上传完成，共上传 {} 个文件", files.length);

        } finally {
            closeConnections(sftpChannel, session);
        }
    }

    // 私有辅助方法

    private Session createSession(ServerConfig server) throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(server.getUsername(), server.getHost(), server.getPort());
        session.setPassword(server.getPassword());

        Properties config = new Properties();
        config.put("StrictHostKeyChecking", "no");
        config.put("PreferredAuthentications", "password");
        session.setConfig(config);
        session.connect(10000); // 10秒超时

        return session;
    }

    private void createRemoteDirectory(ChannelSftp sftpChannel, String remotePath) {
        try {
            String[] pathParts = remotePath.split("/");
            String currentPath = "";

            for (String part : pathParts) {
                if (!part.isEmpty()) {
                    currentPath += "/" + part;
                    try {
                        sftpChannel.mkdir(currentPath);
                    } catch (SftpException e) {
                        log.error(e.getMessage(),e);
                    }
                }
            }
        } catch (Exception e) {
            log.warn("创建远程目录失败: {}", e.getMessage());
        }
    }

    private void closeConnections(ChannelSftp sftpChannel, Session session) {
        if (sftpChannel != null && sftpChannel.isConnected()) {
            sftpChannel.disconnect();
        }
        if (session != null && session.isConnected()) {
            session.disconnect();
        }
    }

    private String getPermissionString(int permissions) {
        StringBuilder sb = new StringBuilder();
        
        // Owner permissions
        sb.append((permissions & 0400) != 0 ? 'r' : '-');
        sb.append((permissions & 0200) != 0 ? 'w' : '-');
        sb.append((permissions & 0100) != 0 ? 'x' : '-');
        
        // Group permissions
        sb.append((permissions & 0040) != 0 ? 'r' : '-');
        sb.append((permissions & 0020) != 0 ? 'w' : '-');
        sb.append((permissions & 0010) != 0 ? 'x' : '-');
        
        // Others permissions
        sb.append((permissions & 0004) != 0 ? 'r' : '-');
        sb.append((permissions & 0002) != 0 ? 'w' : '-');
        sb.append((permissions & 0001) != 0 ? 'x' : '-');
        
        return sb.toString();
    }

    // 文件信息内部类
    public static class FileInfo {
        private String name;
        private boolean isDirectory;
        private long size;
        private long lastModified;
        private String permissions;

        public FileInfo(String name, boolean isDirectory, long size, long lastModified, String permissions) {
            this.name = name;
            this.isDirectory = isDirectory;
            this.size = size;
            this.lastModified = lastModified;
            this.permissions = permissions;
        }

        // Getters
        public String getName() { return name; }
        public boolean isDirectory() { return isDirectory; }
        public long getSize() { return size; }
        public long getLastModified() { return lastModified; }
        public String getPermissions() { return permissions; }
    }
}