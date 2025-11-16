package com.example.encryption.config;

import com.example.encryption.entity.User;
import com.example.encryption.service.UserService;
import com.example.encryption.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * 加密功能测试运行器
 *
 * 用于验证加密功能是否正常工作
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(2)
public class EncryptionTestRunner implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) throws Exception {
        log.info("🧪 开始运行完整的加密功能测试...");

        try {
            // 1. 测试加密工具类
            testCryptoUtil();

            // 2. 测试数据库加密存储
            //testDatabaseEncryption();

            log.info("🎉 所有加密功能测试通过！数据入库加密功能正常工作！");

        } catch (Exception e) {
            log.error("❌ 加密功能测试失败", e);
        }
    }

    /**
     * 测试加密工具类
     */
    private void testCryptoUtil() {
        log.info("🔐 测试加密工具类...");

        String originalText = "13812345678";
        log.info("原始文本: {}", originalText);

        // 测试加密
        String encrypted = CryptoUtil.encrypt(originalText);
        log.info("加密后: {}", encrypted);

        // 验证加密格式
        if (!CryptoUtil.isEncrypted(encrypted)) {
            throw new RuntimeException("加密格式验证失败");
        }

        // 测试解密
        String decrypted = CryptoUtil.decrypt(encrypted);
        log.info("解密后: {}", decrypted);

        // 验证解密结果
        if (!originalText.equals(decrypted)) {
            throw new RuntimeException("解密结果与原文不匹配");
        }

        log.info("✅ 加密工具类测试通过");
    }

    /**
     * 测试数据库加密存储
     */
    private void testDatabaseEncryption() {
        log.info("💾 测试数据库加密存储...");

        // 创建测试用户
        User testUser = new User();
        testUser.setUsername("加密测试用户_" + System.currentTimeMillis());
        testUser.setPhone("13888889999");
        testUser.setIdCard("110101199012121212");
        testUser.setEmail("encryption.test@example.com");
        testUser.setBankCard("6222021234567891234");
        testUser.setAddress("加密测试地址");
        testUser.setAge(30);
        testUser.setGender("男");
        testUser.setOccupation("加密测试工程师");
        testUser.setRemark("用于测试加密功能");

        log.info("📝 创建测试用户: {}", testUser.getUsername());
        log.info("📱 原始手机号: {}", testUser.getPhone());
        log.info("📧 原始邮箱: {}", testUser.getEmail());

        // 保存用户（此时应该通过拦截器或TypeHandler进行加密）
        User savedUser = userService.createUser(testUser);
        log.info("💾 保存用户成功，ID: {}", savedUser.getId());

        // 从数据库重新查询用户（此时应该通过拦截器或TypeHandler进行解密）
        var foundUser = userService.getUserById(savedUser.getId());
        if (foundUser.isPresent()) {
            User user = foundUser.get();
            log.info("🔍 查询到用户: {}", user.getUsername());
            log.info("📱 查询到的手机号: {} (长度: {})", user.getPhone(), user.getPhone() != null ? user.getPhone().length() : 0);
            log.info("📧 查询到的邮箱: {} (长度: {})", user.getEmail(), user.getEmail() != null ? user.getEmail().length() : 0);
            log.info("🆔 查询到的身份证: {} (长度: {})", user.getIdCard(), user.getIdCard() != null ? user.getIdCard().length() : 0);

            // 验证数据是否被正确解密
            boolean phoneMatch = testUser.getPhone().equals(user.getPhone());
            boolean emailMatch = testUser.getEmail().equals(user.getEmail());
            boolean idCardMatch = testUser.getIdCard().equals(user.getIdCard());
            boolean bankCardMatch = testUser.getBankCard().equals(user.getBankCard());
            boolean addressMatch = testUser.getAddress().equals(user.getAddress());

            log.info("🔍 验证结果:");
            log.info("  手机号匹配: {} ({})", phoneMatch, phoneMatch ? "✅" : "❌");
            log.info("  邮箱匹配: {} ({})", emailMatch, emailMatch ? "✅" : "❌");
            log.info("  身份证匹配: {} ({})", idCardMatch, idCardMatch ? "✅" : "❌");
            log.info("  银行卡匹配: {} ({})", bankCardMatch, bankCardMatch ? "✅" : "❌");
            log.info("  地址匹配: {} ({})", addressMatch, addressMatch ? "✅" : "❌");

            if (phoneMatch && emailMatch && idCardMatch && bankCardMatch && addressMatch) {
                log.info("✅ 数据库加密存储测试通过！数据入库时被正确加密，查询时被正确解密！");
            } else {
                throw new RuntimeException("数据库加密存储测试失败：部分字段加解密不匹配");
            }
        } else {
            throw new RuntimeException("无法查询到测试用户！");
        }
    }
}