package com.example.multiport.controller;

import com.example.multiport.annotation.AdminApi;
import com.example.multiport.model.ApiResponse;
import com.example.multiport.model.Product;
import com.example.multiport.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.List;
import java.util.Map;

/**
 * 管理端商品控制器
 * 提供商品管理功能
 */
@Slf4j
@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@AdminApi
public class AdminProductController {

    private final ProductService productService;

    /**
     * 获取所有商品（包括上架和下架）
     */
    @GetMapping
    public ApiResponse<List<Product>> getAllProducts() {
        log.info("管理端获取所有商品列表");
        List<Product> products = productService.getAllProductsForAdmin();
        return ApiResponse.success("获取商品列表成功", products);
    }

    /**
     * 根据ID获取商品详情
     */
    @GetMapping("/{id}")
    public ApiResponse<Product> getProductById(@PathVariable Long id) {
        log.info("管理端获取商品详情，ID: {}", id);
        Product product = productService.getProductById(id);

        if (product == null) {
            return ApiResponse.error(404, "商品不存在");
        }

        return ApiResponse.success("获取商品详情成功", product);
    }

    /**
     * 创建商品
     */
    @PostMapping
    public ApiResponse<Product> createProduct(@Valid @RequestBody Product product) {
        log.info("管理端创建商品: {}", product.getName());

        // 基本验证
        if (product.getName() == null || product.getName().trim().isEmpty()) {
            return ApiResponse.error(400, "商品名称不能为空");
        }

        if (product.getPrice() == null || product.getPrice().doubleValue() <= 0) {
            return ApiResponse.error(400, "商品价格必须大于0");
        }

        if (product.getStock() == null || product.getStock() < 0) {
            return ApiResponse.error(400, "商品库存不能为负数");
        }

        Product createdProduct = productService.createProduct(product);
        return ApiResponse.success("创建商品成功", createdProduct);
    }

    /**
     * 更新商品
     */
    @PutMapping("/{id}")
    public ApiResponse<Product> updateProduct(@PathVariable Long id,
                                            @Valid @RequestBody Product product) {
        log.info("管理端更新商品，ID: {}, 名称: {}", id, product.getName());

        // 基本验证
        if (product.getName() != null && product.getName().trim().isEmpty()) {
            return ApiResponse.error(400, "商品名称不能为空");
        }

        if (product.getPrice() != null && product.getPrice().doubleValue() <= 0) {
            return ApiResponse.error(400, "商品价格必须大于0");
        }

        if (product.getStock() != null && product.getStock() < 0) {
            return ApiResponse.error(400, "商品库存不能为负数");
        }

        Product updatedProduct = productService.updateProduct(id, product);
        if (updatedProduct == null) {
            return ApiResponse.error(404, "商品不存在");
        }

        return ApiResponse.success("更新商品成功", updatedProduct);
    }

    /**
     * 删除商品
     */
    @DeleteMapping("/{id}")
    public ApiResponse<String> deleteProduct(@PathVariable Long id) {
        log.info("管理端删除商品，ID: {}", id);

        boolean deleted = productService.deleteProduct(id);
        if (!deleted) {
            return ApiResponse.error(404, "商品不存在");
        }

        return ApiResponse.success("删除商品成功");
    }

    /**
     * 商品上架/下架
     */
    @PatchMapping("/{id}/status")
    public ApiResponse<Product> updateProductStatus(@PathVariable Long id,
                                                   @RequestBody Map<String, Boolean> request) {
        Boolean status = request.get("status");
        log.info("管理端更新商品状态，ID: {}, 状态: {}", id, status ? "上架" : "下架");

        Product product = productService.getProductById(id);
        if (product == null) {
            return ApiResponse.error(404, "商品不存在");
        }

        product.setStatus(status);
        Product updatedProduct = productService.updateProduct(id, product);

        return ApiResponse.success("更新商品状态成功", updatedProduct);
    }

    /**
     * 批量更新商品状态
     */
    @PatchMapping("/batch/status")
    public ApiResponse<String> batchUpdateStatus(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Long> ids = (List<Long>) request.get("ids");
        Boolean status = (Boolean) request.get("status");

        log.info("管理端批量更新商品状态，商品数量: {}, 状态: {}", ids.size(), status ? "上架" : "下架");

        int successCount = 0;
        for (Long id : ids) {
            Product product = productService.getProductById(id);
            if (product != null) {
                product.setStatus(status);
                productService.updateProduct(id, product);
                successCount++;
            }
        }

        return ApiResponse.success(String.format("批量更新完成，成功更新 %d 个商品", successCount));
    }
}