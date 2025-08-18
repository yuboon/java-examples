package com.example.sqltree;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 演示控制器
 * 提供一些API接口用于测试SQL调用树功能
 */
@Slf4j
@RestController
@RequestMapping("/api")
public class DemoController {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private OrderService orderService;
    
    /**
     * 获取所有用户
     * @return 用户列表
     */
    @GetMapping("/users")
    public Map<String, Object> getAllUsers() {
        log.info("获取所有用户列表");
        
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> users = userService.getAllUsers();
            response.put("success", true);
            response.put("data", users);
            response.put("total", users.size());
        } catch (Exception e) {
            log.error("获取用户列表失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 根据ID获取用户详情(包含订单信息)
     * 这个接口会产生复杂的SQL调用树
     * @param id 用户ID
     * @return 用户详情
     */
    @GetMapping("/users/{id}")
    public Map<String, Object> getUserDetail(@PathVariable Long id) {
        log.info("获取用户详情: id={}", id);
        
        Map<String, Object> response = new HashMap<>();
        try {
            Map<String, Object> userDetail = userService.getUserDetailWithOrders(id);
            if (userDetail != null) {
                response.put("success", true);
                response.put("data", userDetail);
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
            }
        } catch (Exception e) {
            log.error("获取用户详情失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 创建新用户
     * @param request 用户信息
     * @return 创建结果
     */
    @PostMapping("/users")
    public Map<String, Object> createUser(@RequestBody Map<String, String> request) {
        log.info("创建新用户: {}", request);
        
        Map<String, Object> response = new HashMap<>();
        try {
            String username = request.get("username");
            String email = request.get("email");
            String password = request.get("password");
            
            if (username == null || email == null || password == null) {
                response.put("success", false);
                response.put("message", "用户名、邮箱和密码不能为空");
                return response;
            }
            
            Long userId = userService.createUser(username, email, password);
            response.put("success", true);
            response.put("data", Map.of("userId", userId));
            response.put("message", "用户创建成功");
        } catch (Exception e) {
            log.error("创建用户失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 更新用户信息
     * @param id 用户ID
     * @param request 更新信息
     * @return 更新结果
     */
    @PutMapping("/users/{id}")
    public Map<String, Object> updateUser(@PathVariable Long id, @RequestBody Map<String, String> request) {
        log.info("更新用户信息: id={}, request={}", id, request);
        
        Map<String, Object> response = new HashMap<>();
        try {
            String email = request.get("email");
            
            if (email == null) {
                response.put("success", false);
                response.put("message", "邮箱不能为空");
                return response;
            }
            
            boolean updated = userService.updateUser(id, email);
            if (updated) {
                response.put("success", true);
                response.put("message", "用户信息更新成功");
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
            }
        } catch (Exception e) {
            log.error("更新用户信息失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 删除用户
     * @param id 用户ID
     * @return 删除结果
     */
    @DeleteMapping("/users/{id}")
    public Map<String, Object> deleteUser(@PathVariable Long id) {
        log.info("删除用户: id={}", id);
        
        Map<String, Object> response = new HashMap<>();
        try {
            boolean deleted = userService.deleteUser(id);
            if (deleted) {
                response.put("success", true);
                response.put("message", "用户删除成功");
            } else {
                response.put("success", false);
                response.put("message", "用户不存在");
            }
        } catch (Exception e) {
            log.error("删除用户失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 搜索用户
     * @param keyword 搜索关键词
     * @return 搜索结果
     */
    @GetMapping("/users/search")
    public Map<String, Object> searchUsers(@RequestParam String keyword) {
        log.info("搜索用户: keyword={}", keyword);
        
        Map<String, Object> response = new HashMap<>();
        try {
            List<Map<String, Object>> users = userService.searchUsers(keyword);
            response.put("success", true);
            response.put("data", users);
            response.put("total", users.size());
        } catch (Exception e) {
            log.error("搜索用户失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 获取所有订单
     * @return 订单列表
     */
    @GetMapping("/orders")
    public Map<String, Object> getAllOrders() {
        log.info("获取所有订单列表");
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 这里会触发复杂的SQL查询
            List<Map<String, Object>> users = userService.getAllUsers();
            
            // 为每个用户获取订单信息，产生多层SQL调用
            for (Map<String, Object> user : users) {
                Long userId = (Long) user.get("id");
                Map<String, Object> userDetail = userService.getUserDetailWithOrders(userId);
                user.put("orderCount", userDetail != null && userDetail.get("orders") != null ? 
                    ((List<?>) userDetail.get("orders")).size() : 0);
            }
            
            response.put("success", true);
            response.put("data", users);
            response.put("message", "获取订单列表成功");
        } catch (Exception e) {
            log.error("获取订单列表失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 搜索订单
     * @param keyword 搜索关键词
     * @return 搜索结果
     */
    @GetMapping("/orders/search")
    public Map<String, Object> searchOrders(@RequestParam String keyword) {
        log.info("搜索订单: keyword={}", keyword);
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 先搜索用户
            List<Map<String, Object>> users = userService.searchUsers(keyword);
            
            // 为每个匹配的用户获取订单详情
            for (Map<String, Object> user : users) {
                Long userId = (Long) user.get("id");
                Map<String, Object> userDetail = userService.getUserDetailWithOrders(userId);
                if (userDetail != null && userDetail.get("orders") != null) {
                    user.put("orders", userDetail.get("orders"));
                }
            }
            
            response.put("success", true);
            response.put("data", users);
            response.put("total", users.size());
        } catch (Exception e) {
            log.error("搜索订单失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 执行复杂查询测试
     * 这个接口会产生深层次的SQL调用树，用于演示
     * @return 测试结果
     */
    @GetMapping("/test/complex-query")
    public Map<String, Object> complexQueryTest() {
        log.info("执行复杂查询测试");
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 第一层：获取所有用户
            List<Map<String, Object>> users = userService.getAllUsers();
            
            int totalOrders = 0;
            int totalSlowQueries = 0;
            
            // 第二层：为每个用户获取详细信息
            for (Map<String, Object> user : users) {
                Long userId = (Long) user.get("id");
                
                // 第三层：获取用户详情和订单
                Map<String, Object> userDetail = userService.getUserDetailWithOrders(userId);
                
                if (userDetail != null && userDetail.get("orders") != null) {
                    List<?> orders = (List<?>) userDetail.get("orders");
                    totalOrders += orders.size();
                    
                    // 第四层：模拟一些慢查询
                    if (userId % 2 == 0) {
                        try {
                            Thread.sleep(100); // 模拟慢查询
                            totalSlowQueries++;
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                    }
                }
                
                // 第三层：搜索用户（产生更多SQL调用）
                userService.searchUsers(user.get("username").toString());
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("totalUsers", users.size());
            result.put("totalOrders", totalOrders);
            result.put("slowQueries", totalSlowQueries);
            result.put("executionTime", System.currentTimeMillis());
            
            response.put("success", true);
            response.put("data", result);
            response.put("message", "复杂查询测试完成");
        } catch (Exception e) {
            log.error("复杂查询测试失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 真正的递归查询测试
     * 测试新添加的递归方法
     * @return 测试结果
     */
    @GetMapping("/test/real-recursive")
    public Map<String, Object> realRecursiveTest() {
        log.info("执行真正的递归查询测试");
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 使用UserService的真正递归查询方法
            List<Map<String, Object>> result = userService.performRealRecursiveQuery();
            
            response.put("success", true);
            response.put("data", result);
            response.put("message", "真正的递归查询测试完成");
            response.put("maxDepth", "动态递归深度");
        } catch (Exception e) {
            log.error("真正的递归查询测试失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    
    /**
     * 批量操作测试
     * 模拟批量查询和更新操作
     * @return 测试结果
     */
    @GetMapping("/test/batch-operations")
    public Map<String, Object> batchOperationsTest() {
        log.info("执行批量操作测试");
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 批量查询所有用户
            List<Map<String, Object>> allUsers = userService.getAllUsers();
            
            // 为每个用户执行批量操作
            int processedUsers = 0;
            for (Map<String, Object> user : allUsers) {
                Long userId = (Long) user.get("id");
                
                // 批量查询用户详情
                userService.getUserDetailWithOrders(userId);
                
                // 批量搜索操作
                userService.searchUsers(user.get("username").toString());
                userService.searchUsers("test");
                userService.searchUsers("admin");
                
                processedUsers++;
                
                // 限制处理数量，避免过多SQL调用
                if (processedUsers >= 3) {
                    break;
                }
            }
            
            response.put("success", true);
            response.put("processedUsers", processedUsers);
            response.put("totalUsers", allUsers.size());
            response.put("message", "批量操作测试完成");
        } catch (Exception e) {
            log.error("批量操作测试失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 混合查询测试
     * 结合慢SQL、深层嵌套和批量操作
     * @return 测试结果
     */
    @GetMapping("/test/mixed-operations")
    public Map<String, Object> mixedOperationsTest() {
        log.info("执行混合查询测试");
        
        Map<String, Object> response = new HashMap<>();
        try {
            long startTime = System.currentTimeMillis();
            
            // 第一阶段：快速查询
            List<Map<String, Object>> users = userService.getAllUsers();
            
            // 第二阶段：慢查询
            Thread.sleep(300); // 模拟慢查询
            for (int i = 0; i < Math.min(2, users.size()); i++) {
                Map<String, Object> user = users.get(i);
                Long userId = (Long) user.get("id");
                
                // 深层嵌套 + 慢查询
                Thread.sleep(100);
                Map<String, Object> userDetail = userService.getUserDetailWithOrders(userId);
                
                Thread.sleep(150);
                userService.searchUsers(user.get("username").toString());
                
                // 再次深层查询
                if (userDetail != null) {
                    Thread.sleep(80);
                    userService.getUserDetailWithOrders(userId);
                }
            }
            
            // 第三阶段：批量快速查询
            for (Map<String, Object> user : users) {
                userService.searchUsers("test");
                break; // 只执行一次，避免过多调用
            }
            
            long endTime = System.currentTimeMillis();
            
            response.put("success", true);
            response.put("data", users);
            response.put("executionTime", endTime - startTime);
            response.put("message", "混合查询测试完成");
            response.put("phases", "快速查询 -> 慢查询+深层嵌套 -> 批量查询");
        } catch (Exception e) {
            log.error("混合查询测试失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 健康检查接口
     * @return 系统状态
     */
    /**
     * 演示多层级SQL调用的测试接口
     * 产生清晰的层级结构，便于在图上观察
     * @return 测试结果
     */
    @GetMapping("/test/multi-level-demo")
    public Map<String, Object> multiLevelDemo() {
        log.info("执行多层级SQL调用演示");
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 第1层：获取第一个用户
            Map<String, Object> user1 = userService.getUserDetailWithOrders(1L);
            
            // 第1层：获取第二个用户
            Map<String, Object> user2 = userService.getUserDetailWithOrders(2L);
            
            // 第1层：搜索用户
            List<Map<String, Object>> searchResults = userService.searchUsers("admin");
            
            response.put("success", true);
            response.put("message", "多层级SQL调用演示完成");
            response.put("user1", user1);
            response.put("user2", user2);
            response.put("searchResults", searchResults);
            response.put("totalLayers", "预期产生4-5层SQL调用深度");
            
        } catch (Exception e) {
            log.error("多层级SQL调用演示失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 测试Service层调用深度追踪 - 简单场景
     * OrderService调用UserService
     */
    @GetMapping("/test/service-depth/simple")
    public Map<String, Object> testServiceDepthSimple() {
        log.info("测试Service层调用深度追踪 - 简单场景");
        
        Map<String, Object> response = new HashMap<>();
        try {
            // OrderService调用UserService，产生多层SQL调用
            Map<String, Object> orderDetail = orderService.getOrderDetailWithUser(1L);
            
            response.put("success", true);
            response.put("data", orderDetail);
            response.put("message", "OrderService -> UserService 调用完成");
        } catch (Exception e) {
            log.error("测试Service层调用深度追踪失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 测试Service层调用深度追踪 - 复杂场景
     * 多层Service调用嵌套
     */
    @GetMapping("/test/service-depth/complex")
    public Map<String, Object> testServiceDepthComplex() {
        log.info("测试Service层调用深度追踪 - 复杂场景");
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 创建订单，会触发多层Service调用
            Map<String, Object> result = orderService.createOrder(1L,  "2", 99.99);
            
            response.put("success", true);
            response.put("data", result);
            response.put("message", "复杂Service调用链完成");
        } catch (Exception e) {
            log.error("测试复杂Service层调用深度追踪失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
    }
    
    /**
     * 测试Service层调用深度追踪 - 统计场景
     * 获取订单统计信息，涉及多个Service调用
     */
    @GetMapping("/test/service-depth/statistics")
    public Map<String, Object> testServiceDepthStatistics() {
        log.info("测试Service层调用深度追踪 - 统计场景");
        
        Map<String, Object> response = new HashMap<>();
        try {
            // 获取订单统计，会调用多个Service方法
            Map<String, Object> statistics = orderService.getOrderStatistics();
            
            response.put("success", true);
            response.put("data", statistics);
            response.put("message", "订单统计Service调用完成");
        } catch (Exception e) {
            log.error("测试订单统计Service层调用深度追踪失败", e);
            response.put("success", false);
            response.put("message", e.getMessage());
        }
        
        return response;
     }
     
     /**
      * 测试真正的Service调用Service场景
      * 展示完整的Service调用链和SQL深度追踪
      */
     @GetMapping("/test/service-call-chain")
     public Map<String, Object> testServiceCallChain() {
         log.info("测试真正的Service调用Service场景");
         
         Map<String, Object> response = new HashMap<>();
         try {
             // 场景1：OrderService -> UserService 调用链
             Map<String, Object> orderWithUser = orderService.getOrderDetailWithUser(1L);
             
             // 场景2：OrderService -> UserService -> 多层SQL调用
             Map<String, Object> orderStats = orderService.getOrderStatistics();
             
             // 场景3：UserService内部的多层Service调用
             List<Map<String, Object>> recursiveResult = userService.performRealRecursiveQuery();
             
             response.put("success", true);
             response.put("orderWithUser", orderWithUser);
             response.put("orderStats", orderStats);
             response.put("recursiveResult", recursiveResult.size() + " users processed");
             response.put("message", "Service调用链测试完成，包含多层Service嵌套调用");
         } catch (Exception e) {
             log.error("Service调用链测试失败", e);
             response.put("success", false);
             response.put("message", e.getMessage());
         }
         
         return response;
     }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", System.currentTimeMillis());
        response.put("service", "SQL Tree Visualizer");
        return response;
    }
}