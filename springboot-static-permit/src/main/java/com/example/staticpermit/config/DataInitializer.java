package com.example.staticpermit.config;

import com.example.staticpermit.entity.FileInfo;
import com.example.staticpermit.repository.FileInfoRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器，在应用启动时创建示例数据
 */
@Component
public class DataInitializer implements CommandLineRunner {

    @Autowired
    private FileInfoRepository fileInfoRepository;

    @Override
    public void run(String... args) throws Exception {
        // 创建一些示例文件记录
        fileInfoRepository.save(new FileInfo("admin-document.pdf", "admin", "管理员专用文档"));
        fileInfoRepository.save(new FileInfo("user-contract.pdf", "user", "用户合同文件"));
        fileInfoRepository.save(new FileInfo("private-data.xlsx", "admin", "私密数据表格"));
        fileInfoRepository.save(new FileInfo("user-avatar.jpg", "user", "用户头像"));

        System.out.println("Sample file data initialized!");
    }
}