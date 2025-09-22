package com.example.staticpermit.service;

import com.example.staticpermit.entity.FileInfo;
import com.example.staticpermit.repository.FileInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * 文件权限服务，用于判断用户是否有访问文件的权限
 */
@Service
public class FilePermissionService {

    @Autowired
    private FileInfoRepository fileInfoRepository;

    /**
     * 检查用户是否有权限访问指定文件
     * @param authentication 用户认证信息
     * @param filename 文件名
     * @return 是否有权限
     */
    public boolean hasPermission(Authentication authentication, String filename) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        String username = authentication.getName();

        // 管理员可以访问所有文件
        if (authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ADMIN"))) {
            return true;
        }

        // 查询文件信息
        Optional<FileInfo> fileInfo = fileInfoRepository.findByFilename(filename);
        if (fileInfo.isEmpty()) {
            // 文件不存在，拒绝访问
            return false;
        }

        // 只有文件拥有者可以访问
        return username.equals(fileInfo.get().getOwner());
    }
}