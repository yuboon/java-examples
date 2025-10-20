package com.example.multiport.controller;

import com.example.multiport.annotation.UserApi;
import com.example.multiport.model.ApiResponse;
import com.example.multiport.model.CartItem;
import com.example.multiport.service.CartService;
import com.example.multiport.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户端购物车控制器
 * 提供购物车管理功能
 */
@Slf4j
@RestController
@RequestMapping("/cart")
@RequiredArgsConstructor
@UserApi
public class UserCartController {

    private final CartService cartService;
    private final ProductService productService;

    /**
     * 获取用户购物车
     */
    @GetMapping("/{userId}")
    public ApiResponse<List<CartItem>> getUserCart(@PathVariable Long userId) {
        log.info("用户端获取购物车，用户ID: {}", userId);
        List<CartItem> cartItems = cartService.getUserCart(userId);
        return ApiResponse.success("获取购物车成功", cartItems);
    }

    /**
     * 添加商品到购物车
     */
    @PostMapping("/{userId}/items")
    public ApiResponse<CartItem> addToCart(@PathVariable Long userId,
                                          @RequestBody Map<String, Object> request) {
        Long productId = Long.valueOf(request.get("productId").toString());
        Integer quantity = Integer.valueOf(request.get("quantity").toString());

        log.info("用户端添加商品到购物车，用户ID: {}, 商品ID: {}, 数量: {}", userId, productId, quantity);

        // 验证商品是否存在
        var product = productService.getProductById(productId);
        if (product == null || !Boolean.TRUE.equals(product.getStatus())) {
            return ApiResponse.error(404, "商品不存在或已下架");
        }

        // 检查库存
        if (product.getStock() < quantity) {
            return ApiResponse.error(400, "商品库存不足");
        }

        CartItem cartItem = cartService.addToCart(
            userId, productId, product.getName(), quantity, product.getPrice()
        );

        return ApiResponse.success("添加到购物车成功", cartItem);
    }

    /**
     * 更新购物车商品数量
     */
    @PutMapping("/{userId}/items/{cartItemId}")
    public ApiResponse<CartItem> updateCartItem(@PathVariable Long userId,
                                               @PathVariable Long cartItemId,
                                               @RequestBody Map<String, Integer> request) {
        Integer quantity = request.get("quantity");
        log.info("用户端更新购物车商品数量，用户ID: {}, 购物车项ID: {}, 新数量: {}", userId, cartItemId, quantity);

        if (quantity <= 0) {
            return ApiResponse.error(400, "商品数量必须大于0");
        }

        CartItem cartItem = cartService.updateCartItem(userId, cartItemId, quantity);
        if (cartItem == null) {
            return ApiResponse.error(404, "购物车项不存在");
        }

        return ApiResponse.success("更新购物车成功", cartItem);
    }

    /**
     * 从购物车移除商品
     */
    @DeleteMapping("/{userId}/items/{cartItemId}")
    public ApiResponse<String> removeFromCart(@PathVariable Long userId,
                                             @PathVariable Long cartItemId) {
        log.info("用户端从购物车移除商品，用户ID: {}, 购物车项ID: {}", userId, cartItemId);

        boolean removed = cartService.removeFromCart(userId, cartItemId);
        if (!removed) {
            return ApiResponse.error(404, "购物车项不存在");
        }

        return ApiResponse.success("移除商品成功");
    }

    /**
     * 清空购物车
     */
    @DeleteMapping("/{userId}")
    public ApiResponse<String> clearCart(@PathVariable Long userId) {
        log.info("用户端清空购物车，用户ID: {}", userId);
        cartService.clearCart(userId);
        return ApiResponse.success("清空购物车成功");
    }

    /**
     * 获取购物车统计信息
     */
    @GetMapping("/{userId}/summary")
    public ApiResponse<Map<String, Object>> getCartSummary(@PathVariable Long userId) {
        log.info("用户端获取购物车统计信息，用户ID: {}", userId);

        List<CartItem> cartItems = cartService.getUserCart(userId);
        BigDecimal totalAmount = cartService.getCartTotal(userId);
        int itemCount = cartService.getCartItemCount(userId);

        Map<String, Object> summary = new HashMap<>();
        summary.put("items", cartItems);
        summary.put("totalAmount", totalAmount);
        summary.put("itemCount", itemCount);

        return ApiResponse.success("获取购物车统计成功", summary);
    }
}