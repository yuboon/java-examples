package com.example.sqltree;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 订单服务类
 * 用于演示Service层调用和SQL调用树功能
 */
@Slf4j
@Service
public class OrderService {
    
    @Autowired
    private OrderMapper orderMapper;
    
    @Autowired
    private SqlCallTreeContext sqlCallTreeContext;
    
    /**
     * 获取订单详细信息（包含用户信息）
     * 这个方法会调用UserService，产生Service层调用关系
     * @param orderId 订单ID
     * @return 订单详细信息
     */
    public Map<String, Object> getOrderDetailWithUser(Long orderId) {
        log.info("获取订单详细信息: orderId={}", orderId);
        
        // OrderService执行SQL：查询订单基本信息
        Map<String, Object> order = orderMapper.findById(orderId);
        if (order == null) {
            return null;
        }
        
        // OrderService执行SQL：查询订单项
        List<Map<String, Object>> orderItems = orderMapper.findOrderItemsByOrderId(orderId);
        order.put("items", orderItems);
        
        // OrderService执行SQL：查询订单统计
        Map<String, Object> orderStats = orderMapper.getOrderStatistics(orderId);
        order.put("statistics", orderStats);

        
        return order;
    }
    
    /**
     * 处理订单业务逻辑
     * 演示复杂的Service调用关系
     * @param orderId 订单ID
     * @return 处理结果
     */
    public Map<String, Object> processOrder(Long orderId) {
        log.info("处理订单业务: orderId={}", orderId);
        
        Map<String, Object> result = new HashMap<>();
        
        // OrderService执行SQL：查询订单
        Map<String, Object> order = orderMapper.findById(orderId);
        if (order == null) {
            result.put("success", false);
            result.put("message", "订单不存在");
            return result;
        }
        
        // OrderService执行SQL：更新订单状态
        orderMapper.updateStatus(orderId, "PROCESSING");

        
        // OrderService执行SQL：查询订单项
        List<Map<String, Object>> orderItems = orderMapper.findOrderItemsByOrderId(orderId);
        
        // OrderService执行SQL：再次更新订单状态
        orderMapper.updateStatus(orderId, "COMPLETED");
        
        result.put("success", true);
        result.put("order", order);
        result.put("items", orderItems);
        result.put("message", "订单处理成功");
        
        return result;
    }
    
    /**
     * 获取用户的所有订单（调用UserService获取用户信息）
     * @param userId 用户ID
     * @return 用户订单列表
     */
    public Map<String, Object> getUserOrders(Long userId) {
        log.info("获取用户订单列表: userId={}", userId);

        
        // OrderService执行SQL：查询用户订单
        List<Map<String, Object>> orders = orderMapper.findByUserId(userId);
        
        // 为每个订单获取详细信息
        for (Map<String, Object> order : orders) {
            Long orderId = (Long) order.get("id");
            
            // OrderService执行SQL：查询订单项
            List<Map<String, Object>> items = orderMapper.findOrderItemsByOrderId(orderId);
            order.put("items", items);
            
            // OrderService执行SQL：查询订单统计
            Map<String, Object> stats = orderMapper.getOrderStatistics(orderId);
            order.put("statistics", stats);
        }
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);

        result.put("orders", orders);
        result.put("totalOrders", orders.size());
        
        return result;
    }
    
    /**
     * 创建订单（调用UserService验证用户）
     * @param userId 用户ID
     * @param orderNo 订单号
     * @param totalAmount 总金额
     * @return 创建结果
     */
    public Map<String, Object> createOrder(Long userId, String orderNo, Double totalAmount) {
        log.info("创建订单: userId={}, orderNo={}, totalAmount={}", userId, orderNo, totalAmount);
        
        Map<String, Object> result = new HashMap<>();

        // OrderService执行SQL：创建订单
        Long orderId = orderMapper.insert(userId, orderNo, totalAmount);
        
        // OrderService执行SQL：查询新创建的订单
        Map<String, Object> newOrder = orderMapper.findById(orderId);
        
        result.put("success", true);
        result.put("order", newOrder);
        result.put("message", "订单创建成功");
        
        return result;
    }
    
    /**
     * 获取订单统计信息
     * @return 统计信息
     */
    public Map<String, Object> getOrderStatistics() {
        log.info("获取订单统计信息");
        
        // OrderService执行SQL：获取总体统计
        Map<String, Object> overallStats = orderMapper.getOverallStatistics();
        
        // OrderService执行SQL：查询所有订单
        List<Map<String, Object>> allOrders = orderMapper.findAll();
        
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("statistics", overallStats);
        result.put("totalCount", allOrders.size());
        
        return result;
    }
}