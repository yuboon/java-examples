package com.license.service;

import com.license.context.LicenseContext;
import com.license.entity.License;
import com.license.util.HardwareUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Arrays;

/**
 * 许可证启动验证器
 * 在应用启动时验证许可证并保存到上下文
 */
//@Component
public class LicenseStartupValidator implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(LicenseStartupValidator.class);

    @Autowired
    private LicenseService licenseService;

    @Autowired
    private KeyManagementService keyManagementService;

    @Value("${license.file.path:license.json}")
    private String licenseFilePath;

    @Value("${license.public.key.path:public.pem}")
    private String publicKeyPath;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("开始执行许可证启动验证...");

        try {
            // 检查许可证文件是否存在
            Path licensePath = Paths.get(licenseFilePath);
            if (!Files.exists(licensePath)) {
                throw new RuntimeException("许可证文件不存在: " + licenseFilePath);
            }

            // 读取许可证文件内容
            String licenseJson = Files.readString(licensePath, StandardCharsets.UTF_8);
            logger.debug("许可证文件读取成功，文件大小: {} 字节", licenseJson.length());

            // 加载公钥
            Path publicKeyFilePath = Paths.get(publicKeyPath);
            if (Files.exists(publicKeyFilePath)) {
                String publicKeyContent = Files.readString(publicKeyFilePath, StandardCharsets.UTF_8);
                keyManagementService.loadPublicKey(publicKeyContent);
                logger.info("公钥文件加载成功");
            } else {
                throw new RuntimeException("公钥文件不存在: " + publicKeyPath);
            }

            // 验证许可证
            LicenseService.LicenseVerifyResult result = licenseService.verifyLicense(
                licenseJson, keyManagementService.getCachedPublicKey());

            if (!result.isValid()) {
                logger.error("许可证验证失败: {}", result.getMessage());
                throw new RuntimeException("许可证验证失败，应用启动终止: " + result.getMessage());
            }

            // 验证通过，记录许可证信息
            License license = result.getLicense();
            logger.info("====== 许可证验证通过 ======");
            logger.info("软件产品: {}", license.getSubject());
            logger.info("授权对象: {}", license.getIssuedTo());
            logger.info("到期时间: {}", license.getExpireAt());
            logger.info("授权功能: {}", String.join(", ", license.getFeatures()));
            logger.info("绑定硬件: {}", license.getHardwareId());
            logger.info("============================");

            // 将许可证信息保存到应用上下文，供其他组件使用
            LicenseContext.setCurrentLicense(license);

        } catch (Exception e) {
            logger.error("许可证验证过程发生异常，应用启动失败", e);
            throw new RuntimeException("许可证验证失败: " + e.getMessage(), e);
        }
    }
}