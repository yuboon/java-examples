package com.example.multiport.controller;

import com.example.multiport.annotation.UserApi;
import com.example.multiport.model.ApiResponse;
import com.example.multiport.model.Product;
import com.example.multiport.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户端商品控制器
 * 提供用户商品浏览相关功能
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@UserApi
public class UserProductController {

    private final ProductService productService;

    /**
     * 获取所有上架商品
     */
    @GetMapping
    public ApiResponse<List<Product>> getAllProducts() {
        log.info("用户端获取所有商品列表");
        List<Product> products = productService.getAllProductsForUser();
        return ApiResponse.success("获取商品列表成功", products);
    }

    /**
     * 根据ID获取商品详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Product> getProductById(@PathVariable Long id) {
        log.info("用户端获取商品详情，ID: {}", id);
        Product product = productService.getProductById(id);

        if (product == null || !Boolean.TRUE.equals(product.getStatus())) {
            return ApiResponse.error(404, "商品不存在或已下架");
        }

        return ApiResponse.success("获取商品详情成功", product);
    }

    /**
     * 根据分类获取商品
     */
    @GetMapping("/category/{category}")
    public ApiResponse<List<Product>> getProductsByCategory(@PathVariable String category) {
        log.info("用户端根据分类获取商品，分类: {}", category);
        List<Product> products = productService.getProductsByCategory(category);
        return ApiResponse.success("获取分类商品成功", products);
    }

    /**
     * 搜索商品
     */
    @GetMapping("/search")
    public ApiResponse<List<Product>> searchProducts(@RequestParam String keyword) {
        log.info("用户端搜索商品，关键词: {}", keyword);
        List<Product> products = productService.searchProducts(keyword);
        return ApiResponse.success("搜索商品成功", products);
    }
}