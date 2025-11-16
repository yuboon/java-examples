package com.example.encryption.config;

import com.example.encryption.entity.User;
import com.example.encryption.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 示例数据初始化器
 *
 * 功能：
 * - 通过 Java 代码插入示例数据，确保数据经过加密拦截器处理
 * - 避免直接 SQL 插入导致的数据未加密问题
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(1)
public class DataInitializer implements CommandLineRunner {

    private final UserService userService;

    @Override
    public void run(String... args) throws Exception {
        log.info("🔄 开始初始化示例数据...");

        try {
            // 检查是否已有数据
            long userCount = userService.countUsers();
            if (userCount > 0) {
                log.info("📊 数据库已包含 {} 条用户数据，跳过初始化", userCount);
                return;
            }

            // 创建示例用户数据
            createSampleUsers();
            log.info("✅ 示例数据初始化完成");

        } catch (Exception e) {
            log.error("❌ 示例数据初始化失败", e);
            // 不抛出异常，允许应用继续启动
        }
    }

    /**
     * 创建示例用户数据
     */
    private void createSampleUsers() {
        log.info("👥 创建示例用户数据...");

        // 示例用户1
        User user1 = new User();
        user1.setUsername("数据库初始用户");
        user1.setPhone("13899990001");
        user1.setIdCard("110101199009099999");
        user1.setEmail("db.init@example.com");
        user1.setBankCard("6222021234567899999");
        user1.setAddress("北京市海淀区中关村大街1号");
        user1.setAge(35);
        user1.setGender("男");
        user1.setOccupation("系统管理员");
        user1.setRemark("数据库初始化用户 - 展示加密效果");
        userService.createUser(user1);

        // 示例用户2
        User user2 = new User();
        user2.setUsername("示例用户小明");
        user2.setPhone("13899990002");
        user2.setIdCard("110101199010101010");
        user2.setEmail("xiaoming@example.com");
        user2.setBankCard("6222021234567898888");
        user2.setAddress("上海市浦东新区世纪大道200号");
        user2.setAge(26);
        user2.setGender("男");
        user2.setOccupation("Java开发工程师");
        user2.setRemark("数据库初始化用户 - 展示加密效果");
        userService.createUser(user2);

        // 示例用户3
        User user3 = new User();
        user3.setUsername("示例用户小红");
        user3.setPhone("13899990003");
        user3.setIdCard("110101199011111111");
        user3.setEmail("xiaohong@example.com");
        user3.setBankCard("6222021234567897777");
        user3.setAddress("广州市天河区珠江新城100号");
        user3.setAge(24);
        user3.setGender("女");
        user3.setOccupation("前端开发工程师");
        user3.setRemark("数据库初始化用户 - 展示加密效果");
        userService.createUser(user3);

        log.info("👥 成功创建 3 个示例用户");
    }
}