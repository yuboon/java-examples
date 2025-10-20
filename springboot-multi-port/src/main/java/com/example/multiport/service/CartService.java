package com.example.multiport.service;

import com.example.multiport.model.CartItem;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 购物车服务类
 * 模拟购物车相关的业务逻辑
 */
@Service
public class CartService {

    private final Map<Long, List<CartItem>> userCartMap = new ConcurrentHashMap<>();
    private final AtomicLong cartItemIdGenerator = new AtomicLong(1);

    /**
     * 添加商品到购物车
     */
    public CartItem addToCart(Long userId, Long productId, String productName,
                             Integer quantity, BigDecimal price) {
        List<CartItem> cartItems = userCartMap.computeIfAbsent(userId, k -> new ArrayList<>());

        // 检查购物车中是否已存在该商品
        Optional<CartItem> existingItem = cartItems.stream()
                .filter(item -> item.getProductId().equals(productId))
                .findFirst();

        if (existingItem.isPresent()) {
            // 更新数量
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            return item;
        } else {
            // 添加新商品
            CartItem cartItem = new CartItem();
            cartItem.setId(cartItemIdGenerator.getAndIncrement());
            cartItem.setUserId(userId);
            cartItem.setProductId(productId);
            cartItem.setProductName(productName);
            cartItem.setQuantity(quantity);
            cartItem.setPrice(price);
            cartItem.setAddTime(LocalDateTime.now());

            cartItems.add(cartItem);
            return cartItem;
        }
    }

    /**
     * 获取用户购物车
     */
    public List<CartItem> getUserCart(Long userId) {
        return userCartMap.getOrDefault(userId, new ArrayList<>());
    }

    /**
     * 更新购物车商品数量
     */
    public CartItem updateCartItem(Long userId, Long cartItemId, Integer quantity) {
        List<CartItem> cartItems = userCartMap.get(userId);
        if (cartItems == null) {
            return null;
        }

        Optional<CartItem> itemOptional = cartItems.stream()
                .filter(item -> item.getId().equals(cartItemId))
                .findFirst();

        if (itemOptional.isPresent()) {
            CartItem item = itemOptional.get();
            item.setQuantity(quantity);
            return item;
        }

        return null;
    }

    /**
     * 从购物车移除商品
     */
    public boolean removeFromCart(Long userId, Long cartItemId) {
        List<CartItem> cartItems = userCartMap.get(userId);
        if (cartItems == null) {
            return false;
        }

        return cartItems.removeIf(item -> item.getId().equals(cartItemId));
    }

    /**
     * 清空购物车
     */
    public void clearCart(Long userId) {
        userCartMap.remove(userId);
    }

    /**
     * 获取购物车总金额
     */
    public BigDecimal getCartTotal(Long userId) {
        List<CartItem> cartItems = getUserCart(userId);
        return cartItems.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * 获取购物车商品总数
     */
    public int getCartItemCount(Long userId) {
        List<CartItem> cartItems = getUserCart(userId);
        return cartItems.stream()
                .mapToInt(CartItem::getQuantity)
                .sum();
    }
}