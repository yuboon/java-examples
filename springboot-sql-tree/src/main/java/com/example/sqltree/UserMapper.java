package com.example.sqltree;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 用户数据访问层
 * 使用MyBatis注解方式定义SQL
 */
@Mapper
@Repository
public interface UserMapper {
    
    /**
     * 根据ID查询用户
     * @param id 用户ID
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") Long id);
    
    /**
     * 根据用户名查询用户
     * @param username 用户名
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE username = #{username}")
    Map<String, Object> findByUsername(@Param("username") String username);
    
    /**
     * 根据邮箱查询用户
     * @param email 邮箱
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE email = #{email}")
    Map<String, Object> findByEmail(@Param("email") String email);
    
    /**
     * 根据邮箱查询用户(排除指定用户)
     * @param email 邮箱
     * @param excludeUserId 排除的用户ID
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE email = #{email} AND id != #{excludeUserId}")
    Map<String, Object> findByEmailExcludeUser(@Param("email") String email, @Param("excludeUserId") Long excludeUserId);
    
    /**
     * 查询所有用户
     * @return 用户列表
     */
    @Select("SELECT id, username, email, status, created_time, updated_time FROM users ORDER BY created_time DESC")
    List<Map<String, Object>> findAll();
    
    /**
     * 插入新用户
     * @param username 用户名
     * @param email 邮箱
     * @param password 密码
     * @return 新用户ID
     */
    @Insert("INSERT INTO users (username, email, password) VALUES (#{username}, #{email}, #{password})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Long insert(@Param("username") String username, @Param("email") String email, @Param("password") String password);
    
    /**
     * 更新用户邮箱
     * @param id 用户ID
     * @param email 新邮箱
     * @return 影响行数
     */
    @Update("UPDATE users SET email = #{email}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateEmail(@Param("id") Long id, @Param("email") String email);
    
    /**
     * 根据ID删除用户
     * @param id 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM users WHERE id = #{id}")
    int deleteById(@Param("id") Long id);
    
    /**
     * 根据用户名搜索用户
     * @param keyword 关键词
     * @return 用户列表
     */
    @Select("SELECT id, username, email, status, created_time FROM users WHERE username LIKE CONCAT('%', #{keyword}, '%') ORDER BY username")
    List<Map<String, Object>> searchByUsername(@Param("keyword") String keyword);
    
    /**
     * 根据邮箱搜索用户
     * @param keyword 关键词
     * @return 用户列表
     */
    @Select("SELECT id, username, email, status, created_time FROM users WHERE email LIKE CONCAT('%', #{keyword}, '%') ORDER BY email")
    List<Map<String, Object>> searchByEmail(@Param("keyword") String keyword);
    
    /**
     * 获取用户统计信息
     * @param userId 用户ID
     * @return 统计信息
     */
    @Select("""
                SELECT 
                COUNT(o.id) as order_count,
                COALESCE(SUM(o.total_amount), 0) as total_amount,
                COALESCE(AVG(o.total_amount), 0) as avg_amount,
                COUNT(CASE WHEN o.status = 'COMPLETED' THEN 1 END) as completed_orders,
                COUNT(CASE WHEN o.status = 'PENDING' THEN 1 END) as pending_orders
              FROM users u 
              LEFT JOIN orders o ON u.id = o.user_id 
              WHERE u.id = #{userId}
              GROUP BY u.id
              """)
    Map<String, Object> getUserStatistics(@Param("userId") Long userId);
    
    /**
     * 插入用户日志
     * @param userId 用户ID
     * @param action 操作类型
     * @param description 描述
     * @return 影响行数
     */
    @Insert("""
              INSERT INTO user_logs (user_id, action, description, created_time) 
              VALUES (#{userId}, #{action}, #{description}, CURRENT_TIMESTAMP)""")
    int insertUserLog(@Param("userId") Long userId, @Param("action") String action, @Param("description") String description);
    
    /**
     * 删除用户日志
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM user_logs WHERE user_id = #{userId}")
    int deleteUserLogsByUserId(@Param("userId") Long userId);
    
    /**
     * 创建用户日志表(如果不存在)
     */
    @Update("""
                CREATE TABLE IF NOT EXISTS user_logs (
                id BIGINT PRIMARY KEY AUTO_INCREMENT,
                user_id BIGINT NOT NULL,
                action VARCHAR(50) NOT NULL,
                description TEXT,
                created_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (user_id) REFERENCES users(id)
              )""")
    void createUserLogsTable();
    
    /**
     * 获取用户完整信息（使用子查询模拟嵌套SQL调用）
     * @param userId 用户ID
     * @return 用户完整信息
     */
    @Select("""
                SELECT 
                u.*,
                (
                    SELECT COUNT(*) 
                    FROM orders o 
                    WHERE o.user_id = u.id
                ) as order_count,
                (
                    SELECT COALESCE(SUM(o.total_amount), 0) 
                    FROM orders o 
                    WHERE o.user_id = u.id
                ) as total_amount,
                (
                    SELECT COUNT(*) 
                    FROM orders o 
                    WHERE o.user_id = u.id AND o.status = 'COMPLETED'
                ) as completed_orders,
                (
                    SELECT COUNT(oi.id) 
                    FROM orders o 
                    JOIN order_items oi ON o.id = oi.order_id 
                    WHERE o.user_id = u.id
                ) as total_items
                FROM users u 
                WHERE u.id = #{userId}
              """)
    Map<String, Object> getUserCompleteInfo(@Param("userId") Long userId);
    
    /**
     * 获取用户信息并触发嵌套查询（使用ResultMap）
     * @param userId 用户ID
     * @return 用户信息
     */
    @Select("SELECT * FROM users WHERE id = #{userId}")
    @Results({
        @Result(property = "id", column = "id"),
        @Result(property = "username", column = "username"),
        @Result(property = "email", column = "email"),
        @Result(property = "orders", column = "id", 
                javaType = List.class,
                many = @Many(select = "com.example.sqltree.OrderMapper.findByUserId"))
    })
    Map<String, Object> getUserWithNestedOrders(@Param("userId") Long userId);
}