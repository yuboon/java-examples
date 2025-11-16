package com.example.encryption.service;

import com.example.encryption.entity.User;
import com.example.encryption.mapper.UserMapper;
import com.example.encryption.util.CryptoUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * 用户服务层
 *
 * 功能：
 * - 提供用户相关的业务逻辑
 * - 处理加密字段的查询逻辑
 * - 事务管理
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserMapper userMapper;

    /**
     * 创建用户
     */
    @Transactional
    public User createUser(User user) {
        log.info("创建用户: {}", user.getUsername());

        // 设置时间戳
        LocalDateTime now = LocalDateTime.now();
        user.setCreateTime(now);
        user.setUpdateTime(now);
        user.setEnabled(true);

        // 插入数据库（加密字段会自动加密）
        int result = userMapper.insert(user);
        if (result > 0) {
            log.info("用户创建成功，ID: {}", user.getId());
            return user;
        } else {
            throw new RuntimeException("用户创建失败");
        }
    }

    /**
     * 批量创建用户
     */
    @Transactional
    public List<User> createUsers(List<User> users) {
        log.info("批量创建用户，数量: {}", users.size());

        // 设置时间戳
        LocalDateTime now = LocalDateTime.now();
        users.forEach(user -> {
            user.setCreateTime(now);
            user.setUpdateTime(now);
            user.setEnabled(true);
        });

        // 批量插入
        int result = userMapper.batchInsert(users);
        if (result == users.size()) {
            log.info("批量用户创建成功，数量: {}", result);
            return users;
        } else {
            throw new RuntimeException("批量用户创建失败，预期: " + users.size() + "，实际: " + result);
        }
    }

    /**
     * 根据ID查询用户
     */
    public Optional<User> getUserById(Long id) {
        log.debug("查询用户，ID: {}", id);
        return userMapper.findById(id);
    }

    /**
     * 根据用户名查询用户
     */
    public Optional<User> getUserByUsername(String username) {
        log.debug("查询用户，用户名: {}", username);
        return userMapper.findByUsername(username);
    }

    /**
     * 根据手机号查询用户
     * 注意：由于手机号在数据库中是加密存储的，需要先加密再查询
     */
    public Optional<User> getUserByPhone(String phone) {
        log.debug("查询用户，手机号: {}", phone);

        try {
            // 先加密手机号，再查询
            String encryptedPhone = CryptoUtil.encrypt(phone);
            return userMapper.findByPhone(encryptedPhone);
        } catch (Exception e) {
            log.error("查询用户失败，手机号: {}", phone, e);
            return Optional.empty();
        }
    }

    /**
     * 根据邮箱查询用户
     */
    public Optional<User> getUserByEmail(String email) {
        log.debug("查询用户，邮箱: {}", email);

        try {
            // 先加密邮箱，再查询
            String encryptedEmail = CryptoUtil.encrypt(email);
            return userMapper.findByEmail(encryptedEmail);
        } catch (Exception e) {
            log.error("查询用户失败，邮箱: {}", email, e);
            return Optional.empty();
        }
    }

    /**
     * 查询所有用户
     */
    public List<User> getAllUsers() {
        log.debug("查询所有用户");
        return userMapper.findAll();
    }

    /**
     * 分页查询用户
     */
    public List<User> getUsersByPage(int page, int size) {
        log.debug("分页查询用户，页码: {}, 每页大小: {}", page, size);
        int offset = (page - 1) * size;
        return userMapper.findByPage(offset, size);
    }

    /**
     * 更新用户信息
     */
    @Transactional
    public User updateUser(User user) {
        log.info("更新用户，ID: {}", user.getId());

        // 设置更新时间
        user.setUpdateTime(LocalDateTime.now());

        // 更新数据库（加密字段会自动加密）
        int result = userMapper.update(user);
        if (result > 0) {
            log.info("用户更新成功，ID: {}", user.getId());
            return user;
        } else {
            throw new RuntimeException("用户更新失败，ID: " + user.getId());
        }
    }

    /**
     * 部分更新用户信息
     */
    @Transactional
    public User updateUserSelective(User user) {
        log.info("部分更新用户，ID: {}", user.getId());

        // 设置更新时间
        user.setUpdateTime(LocalDateTime.now());

        // 更新数据库（加密字段会自动加密）
        int result = userMapper.updateSelective(user);
        if (result > 0) {
            log.info("用户部分更新成功，ID: {}", user.getId());
            // 重新查询完整的用户信息
            return getUserById(user.getId())
                    .orElseThrow(() -> new RuntimeException("更新后查询用户失败，ID: " + user.getId()));
        } else {
            throw new RuntimeException("用户部分更新失败，ID: " + user.getId());
        }
    }

    /**
     * 删除用户
     */
    @Transactional
    public boolean deleteUser(Long id) {
        log.info("删除用户，ID: {}", id);
        int result = userMapper.deleteById(id);
        boolean success = result > 0;
        if (success) {
            log.info("用户删除成功，ID: {}", id);
        } else {
            log.warn("用户删除失败，ID: {}", id);
        }
        return success;
    }

    /**
     * 批量删除用户
     */
    @Transactional
    public int batchDeleteUsers(List<Long> ids) {
        log.info("批量删除用户，数量: {}", ids.size());
        int result = userMapper.batchDeleteByIds(ids);
        log.info("批量删除用户完成，成功: {}", result);
        return result;
    }

    /**
     * 统计用户总数
     */
    public long countUsers() {
        log.debug("统计用户总数");
        return userMapper.count();
    }

    /**
     * 根据条件统计用户数量
     */
    public long countUsersByCondition(String username, Boolean enabled, Integer age, String gender) {
        log.debug("根据条件统计用户数量");
        return userMapper.countByCondition(username, enabled, age, gender);
    }

    /**
     * 根据条件查询用户
     */
    public List<User> searchUsers(String username, Boolean enabled, Integer age, String gender, int page, int size) {
        log.debug("根据条件查询用户");
        int offset = (page - 1) * size;
        return userMapper.findByCondition(username, enabled, age, gender, offset, size);
    }

    /**
     * 启用/禁用用户
     */
    @Transactional
    public User toggleUserStatus(Long id, boolean enabled) {
        log.info("切换用户状态，ID: {}, 状态: {}", id, enabled);

        User user = getUserById(id)
                .orElseThrow(() -> new RuntimeException("用户不存在，ID: " + id));

        user.setEnabled(enabled);
        user.setUpdateTime(LocalDateTime.now());

        return updateUserSelective(user);
    }

    /**
     * 检查用户是否存在
     */
    public boolean userExists(Long id) {
        return getUserById(id).isPresent();
    }

    /**
     * 检查用户名是否存在
     */
    public boolean usernameExists(String username) {
        return getUserByUsername(username).isPresent();
    }

    /**
     * 检查手机号是否存在
     */
    public boolean phoneExists(String phone) {
        return getUserByPhone(phone).isPresent();
    }

    /**
     * 检查邮箱是否存在
     */
    public boolean emailExists(String email) {
        return getUserByEmail(email).isPresent();
    }
}