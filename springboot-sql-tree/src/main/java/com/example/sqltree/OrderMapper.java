package com.example.sqltree;

import org.apache.ibatis.annotations.*;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * 订单数据访问层
 * 使用MyBatis注解方式定义SQL
 */
@Mapper
@Repository
public interface OrderMapper {
    
    /**
     * 根据用户ID查询订单
     * @param userId 用户ID
     * @return 订单列表
     */
    @Select("SELECT * FROM orders WHERE user_id = #{userId} ORDER BY created_time DESC")
    List<Map<String, Object>> findByUserId(@Param("userId") Long userId);
    
    /**
     * 根据订单ID查询订单详情
     * @param orderId 订单ID
     * @return 订单详情列表
     */
    @Select("SELECT * FROM order_items WHERE order_id = #{orderId} ORDER BY id")
    List<Map<String, Object>> findOrderItemsByOrderId(@Param("orderId") Long orderId);
    
    /**
     * 获取订单统计信息
     * @param orderId 订单ID
     * @return 统计信息
     */
    @Select("""
                SELECT 
                COUNT(oi.id) as item_count,
                SUM(oi.quantity) as total_quantity,
                SUM(oi.quantity * oi.price) as calculated_total,
                AVG(oi.price) as avg_price,
                MIN(oi.price) as min_price,
                MAX(oi.price) as max_price
              FROM order_items oi 
              WHERE oi.order_id = #{orderId}""")
    Map<String, Object> getOrderStatistics(@Param("orderId") Long orderId);
    
    /**
     * 查询用户的待处理订单
     * @param userId 用户ID
     * @return 待处理订单列表
     */
    @Select("SELECT * FROM orders WHERE user_id = #{userId} AND status IN ('PENDING', 'PROCESSING') ORDER BY created_time")
    List<Map<String, Object>> findPendingOrdersByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID删除订单详情
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("""
              DELETE oi FROM order_items oi 
              INNER JOIN orders o ON oi.order_id = o.id 
              WHERE o.user_id = #{userId}""")
    int deleteOrderItemsByUserId(@Param("userId") Long userId);
    
    /**
     * 根据用户ID删除订单
     * @param userId 用户ID
     * @return 影响行数
     */
    @Delete("DELETE FROM orders WHERE user_id = #{userId}")
    int deleteOrdersByUserId(@Param("userId") Long userId);
    
    /**
     * 根据ID查询订单
     * @param id 订单ID
     * @return 订单信息
     */
    @Select("SELECT * FROM orders WHERE id = #{id}")
    Map<String, Object> findById(@Param("id") Long id);
    
    /**
     * 查询所有订单
     * @return 订单列表
     */
    @Select("""
              SELECT o.*, u.username, u.email 
              FROM orders o 
              LEFT JOIN users u ON o.user_id = u.id 
              ORDER BY o.created_time DESC""")
    List<Map<String, Object>> findAll();
    
    /**
     * 根据订单号查询订单
     * @param orderNo 订单号
     * @return 订单信息
     */
    @Select("SELECT * FROM orders WHERE order_no = #{orderNo}")
    Map<String, Object> findByOrderNo(@Param("orderNo") String orderNo);
    
    /**
     * 创建新订单
     * @param userId 用户ID
     * @param orderNo 订单号
     * @param totalAmount 总金额
     * @return 新订单ID
     */
    @Insert("INSERT INTO orders (user_id, order_no, total_amount) VALUES (#{userId}, #{orderNo}, #{totalAmount})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    Long insert(@Param("userId") Long userId, @Param("orderNo") String orderNo, @Param("totalAmount") Double totalAmount);
    
    /**
     * 更新订单状态
     * @param id 订单ID
     * @param status 新状态
     * @return 影响行数
     */
    @Update("UPDATE orders SET status = #{status}, updated_time = CURRENT_TIMESTAMP WHERE id = #{id}")
    int updateStatus(@Param("id") Long id, @Param("status") String status);
    
    /**
     * 添加订单详情
     * @param orderId 订单ID
     * @param productName 产品名称
     * @param quantity 数量
     * @param price 价格
     * @return 影响行数
     */
    @Insert("INSERT INTO order_items (order_id, product_name, quantity, price) VALUES (#{orderId}, #{productName}, #{quantity}, #{price})")
    int insertOrderItem(@Param("orderId") Long orderId, @Param("productName") String productName, 
                       @Param("quantity") Integer quantity, @Param("price") Double price);
    
    /**
     * 根据状态查询订单
     * @param status 订单状态
     * @return 订单列表
     */
    @Select("""
              SELECT o.*, u.username, u.email 
              FROM orders o 
              LEFT JOIN users u ON o.user_id = u.id 
              WHERE o.status = #{status} 
              ORDER BY o.created_time DESC""")
    List<Map<String, Object>> findByStatus(@Param("status") String status);
    
    /**
     * 获取订单总体统计信息
     * @return 统计信息
     */
    @Select("""
                SELECT 
                COUNT(*) as total_orders,
                SUM(total_amount) as total_revenue,
                AVG(total_amount) as avg_order_value,
                COUNT(CASE WHEN status = 'COMPLETED' THEN 1 END) as completed_orders,
                COUNT(CASE WHEN status = 'PENDING' THEN 1 END) as pending_orders,
                COUNT(CASE WHEN status = 'SHIPPING' THEN 1 END) as shipping_orders,
                COUNT(CASE WHEN status = 'CANCELLED' THEN 1 END) as cancelled_orders
              FROM orders""")
    Map<String, Object> getOverallStatistics();
    
    /**
     * 根据日期范围查询订单
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 订单列表
     */
    @Select("""
              SELECT o.*, u.username, u.email 
              FROM orders o 
              LEFT JOIN users u ON o.user_id = u.id 
              WHERE o.created_time BETWEEN #{startDate} AND #{endDate} 
              ORDER BY o.created_time DESC""")
    List<Map<String, Object>> findByDateRange(@Param("startDate") String startDate, @Param("endDate") String endDate);
    
    /**
     * 搜索订单
     * @param keyword 关键词
     * @return 订单列表
     */
    @Select("""
              SELECT o.*, u.username, u.email 
              FROM orders o 
              LEFT JOIN users u ON o.user_id = u.id 
              WHERE o.order_no LIKE CONCAT('%', #{keyword}, '%') 
                 OR u.username LIKE CONCAT('%', #{keyword}, '%') 
                 OR u.email LIKE CONCAT('%', #{keyword}, '%')
              ORDER BY o.created_time DESC""")
    List<Map<String, Object>> searchOrders(@Param("keyword") String keyword);
}