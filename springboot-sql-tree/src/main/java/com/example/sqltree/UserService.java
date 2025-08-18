package com.example.sqltree;

import lombok.extern.slf4j.Slf4j;
import com.example.sqltree.SqlCallTreeContext;
import com.example.sqltree.SqlNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * 用户服务类
 * 用于演示SQL调用树功能
 */
@Slf4j
@Service
public class UserService {
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private OrderMapper orderMapper;

    @Autowired
    private SqlCallTreeContext sqlCallTreeContext;

    @Autowired
    private OrderService orderService;
    
    /**
     * 获取用户详细信息(包含订单)
     * 这个方法会产生多层SQL调用，用于演示调用树
     * @param userId 用户ID
     * @return 用户详细信息
     */
    public Map<String, Object> getUserDetailWithOrders(Long userId) {
        log.info("获取用户详细信息: userId={}", userId);
        
        // 第一层：查询用户基本信息
        Map<String, Object> user = userMapper.findById(userId);
        if (user == null) {
            return null;
        }
        
        // 第二层：查询用户的所有订单
        Map<String, Object> order = orderService.getUserOrders(userId);
        user.put("orders", order);

        Map<String, Object> orderItem = orderService.getOrderDetailWithUser(Long.parseLong(String.valueOf(order.get("id"))));
        user.put("items", orderItem);

        // 第二层：查询用户统计信息
        Map<String, Object> userStats = userMapper.getUserStatistics(userId);
        user.put("statistics", userStats);
        
        return user;
    }
    
    /**
     * 获取所有用户列表
     * @return 用户列表
     */
    public List<Map<String, Object>> getAllUsers() {
        log.info("获取所有用户列表");
        return userMapper.findAll();
    }
    
    /**
     * 获取用户基本信息
     * @param userId 用户ID
     * @return 用户基本信息
     */
    public Map<String, Object> getUserBasicInfo(Long userId) {
        log.info("获取用户基本信息: userId={}", userId);
        return userMapper.findById(userId);
    }
    
    /**
     * 获取用户统计信息
     * @param userId 用户ID
     * @return 用户统计信息
     */
    public Map<String, Object> getUserStatistics(Long userId) {
        log.info("获取用户统计信息: userId={}", userId);
        return userMapper.getUserStatistics(userId);
    }
    
    /**
     * 创建新用户
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     * @return 创建的用户ID
     */
    public Long createUser(String username, String email, String password) {
        log.info("创建新用户: username={}, email={}", username, email);
        
        // 检查用户名是否已存在
        Map<String, Object> existingUser = userMapper.findByUsername(username);
        if (existingUser != null) {
            throw new RuntimeException("用户名已存在: " + username);
        }
        
        // 检查邮箱是否已存在
        Map<String, Object> existingEmail = userMapper.findByEmail(email);
        if (existingEmail != null) {
            throw new RuntimeException("邮箱已存在: " + email);
        }
        
        // 创建用户
        Long userId = userMapper.insert(username, email, password);
        
        // 记录用户创建日志
        userMapper.insertUserLog(userId, "USER_CREATED", "用户创建成功");
        
        return userId;
    }
    
    /**
     * 更新用户信息
     * @param userId 用户ID
     * @param email 新邮箱
     * @return 是否更新成功
     */
    public boolean updateUser(Long userId, String email) {
        log.info("更新用户信息: userId={}, email={}", userId, email);
        
        // 检查用户是否存在
        Map<String, Object> user = userMapper.findById(userId);
        if (user == null) {
            return false;
        }
        
        // 检查新邮箱是否已被其他用户使用
        Map<String, Object> existingEmail = userMapper.findByEmailExcludeUser(email, userId);
        if (existingEmail != null) {
            throw new RuntimeException("邮箱已被其他用户使用: " + email);
        }
        
        // 更新用户信息
        int updated = userMapper.updateEmail(userId, email);
        
        if (updated > 0) {
            // 记录更新日志
            userMapper.insertUserLog(userId, "USER_UPDATED", "用户信息更新成功");
        }
        
        return updated > 0;
    }
    
    /**
     * 删除用户
     * @param userId 用户ID
     * @return 是否删除成功
     */
    public boolean deleteUser(Long userId) {
        log.info("删除用户: userId={}", userId);

        // 删除用户
        int deleted = userMapper.deleteById(userId);
        
        return deleted > 0;
    }
    
    /**
     * 搜索用户
     * @param keyword 关键词
     * @return 搜索结果
     */
    public List<Map<String, Object>> searchUsers(String keyword) {
        log.info("搜索用户: keyword={}", keyword);
        
        // 按用户名搜索
        List<Map<String, Object>> usersByUsername = userMapper.searchByUsername(keyword);
        
        // 按邮箱搜索
        List<Map<String, Object>> usersByEmail = userMapper.searchByEmail(keyword);
        
        // 合并结果并去重
        usersByUsername.addAll(usersByEmail);
        
        return usersByUsername.stream()
                .distinct()
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 执行真正的递归深层嵌套查询
     * 使用真正的Service调用而非手动enter
     * @return 查询结果
     */
    @Transactional
    public List<Map<String, Object>> performRealRecursiveQuery() {
        log.info("开始执行真正的递归深层嵌套查询");

        // 第一层：UserService获取所有用户
        List<Map<String, Object>> users = userMapper.findAll();
        log.info("第1层Service调用完成，获取到{}个用户", users.size());
        
        // 使用跨Service调用创建嵌套层级
        if (!users.isEmpty()) {
            Long firstUserId = (Long) users.get(0).get("id");

            
            // 第二层：UserService调用OrderService获取用户订单
            Map<String, Object> userOrders = orderService.getUserOrders(firstUserId);
            log.info("第2层Service调用完成，UserService调用OrderService获取用户订单");
            
            if (userOrders != null && userOrders.get("orders") != null) {
                List<Map<String, Object>> orders = (List<Map<String, Object>>) userOrders.get("orders");
                if (!orders.isEmpty()) {
                    Long firstOrderId = (Long) orders.get(0).get("id");
                    Map<String, Object> processResult = orderService.processOrder(firstOrderId);

                    // 第五层：UserService调用OrderService获取订单详情（包含用户信息）
                    Map<String, Object> orderDetail = orderService.getOrderDetailWithUser(firstOrderId);
                    log.info("第2层Service调用完成，UserService调用OrderService获取订单详情");
                }
            }
            
            Map<String, Object> statistics = userMapper.getUserStatistics(firstUserId);
            log.info("第6层SQL调用完成，获取用户统计信息");
        }
        
        log.info("递归嵌套查询执行完成");
        
        return users;
    }

}