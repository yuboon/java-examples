package com.example.staticpermit.controller;

import com.example.staticpermit.service.FilePermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.MalformedURLException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 方案二：自定义控制器代理文件访问
 * 通过Controller来代理私有文件的访问请求，实现复杂的权限控制逻辑
 */
@RestController
@RequestMapping("/files")
public class Solution2PrivateFileController {

    private static final String PRIVATE_STORAGE_PATH = "private-uploads/";

    @Autowired
    private FilePermissionService filePermissionService;

    @GetMapping("/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable String filename) {
        // 1. 获取当前登录用户信息
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        // 2. 执行权限检查
        if (!filePermissionService.hasPermission(authentication, filename)) {
            // 如果无权访问，返回403 Forbidden
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        try {
            // 3. 加载文件资源
            Path file = Paths.get(PRIVATE_STORAGE_PATH).resolve(filename);
            Resource resource = new UrlResource(file.toUri());

            if (resource.exists() && resource.isReadable()) {
                // 4. 设置响应头，让浏览器能正确处理文件
                return ResponseEntity.ok()
                        .header(HttpHeaders.CONTENT_DISPOSITION,
                               "attachment; filename=\"" + resource.getFilename() + "\"")
                        .body(resource);
            } else {
                // 文件不存在或无法读取
                return ResponseEntity.notFound().build();
            }
        } catch (MalformedURLException e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}