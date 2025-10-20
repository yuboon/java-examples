package com.example.multiport.service;

import com.example.multiport.model.Product;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

/**
 * 商品服务类
 * 模拟商品相关的业务逻辑
 */
@Service
public class ProductService {

    private final Map<Long, Product> productMap = new HashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    public ProductService() {
        // 初始化一些测试数据
        initTestData();
    }

    /**
     * 获取所有商品（用户端 - 只返回上架商品）
     */
    public List<Product> getAllProductsForUser() {
        return productMap.values().stream()
                .filter(product -> Boolean.TRUE.equals(product.getStatus()))
                .sorted(Comparator.comparing(Product::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 获取所有商品（管理端 - 返回所有商品）
     */
    public List<Product> getAllProductsForAdmin() {
        return new ArrayList<>(productMap.values())
                .stream()
                .sorted(Comparator.comparing(Product::getCreateTime).reversed())
                .collect(Collectors.toList());
    }

    /**
     * 根据ID获取商品
     */
    public Product getProductById(Long id) {
        return productMap.get(id);
    }

    /**
     * 创建商品
     */
    public Product createProduct(Product product) {
        long id = idGenerator.getAndIncrement();
        product.setId(id);
        product.setCreateTime(LocalDateTime.now());
        product.setUpdateTime(LocalDateTime.now());

        if (product.getStatus() == null) {
            product.setStatus(true);
        }

        productMap.put(id, product);
        return product;
    }

    /**
     * 更新商品
     */
    public Product updateProduct(Long id, Product product) {
        Product existingProduct = productMap.get(id);
        if (existingProduct == null) {
            return null;
        }

        existingProduct.setName(product.getName());
        existingProduct.setDescription(product.getDescription());
        existingProduct.setPrice(product.getPrice());
        existingProduct.setStock(product.getStock());
        existingProduct.setCategory(product.getCategory());
        existingProduct.setStatus(product.getStatus());
        existingProduct.setUpdateTime(LocalDateTime.now());

        return existingProduct;
    }

    /**
     * 删除商品
     */
    public boolean deleteProduct(Long id) {
        return productMap.remove(id) != null;
    }

    /**
     * 根据分类获取商品
     */
    public List<Product> getProductsByCategory(String category) {
        return productMap.values().stream()
                .filter(product -> category.equals(product.getCategory()))
                .filter(product -> Boolean.TRUE.equals(product.getStatus()))
                .collect(Collectors.toList());
    }

    /**
     * 搜索商品
     */
    public List<Product> searchProducts(String keyword) {
        String lowerKeyword = keyword.toLowerCase();
        return productMap.values().stream()
                .filter(product -> Boolean.TRUE.equals(product.getStatus()))
                .filter(product ->
                    product.getName().toLowerCase().contains(lowerKeyword) ||
                    product.getDescription().toLowerCase().contains(lowerKeyword)
                )
                .collect(Collectors.toList());
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        long totalProducts = productMap.size();
        long activeProducts = productMap.values().stream()
                .filter(p -> Boolean.TRUE.equals(p.getStatus()))
                .count();
        long totalStock = productMap.values().stream()
                .mapToInt(Product::getStock)
                .sum();

        Map<String, Object> stats = new HashMap<>();
        stats.put("totalProducts", totalProducts);
        stats.put("activeProducts", activeProducts);
        stats.put("inactiveProducts", totalProducts - activeProducts);
        stats.put("totalStock", totalStock);
        stats.put("categories", productMap.values().stream()
                .map(Product::getCategory)
                .distinct()
                .collect(Collectors.toList()));

        return stats;
    }

    /**
     * 初始化测试数据
     */
    private void initTestData() {
        Product product1 = new Product();
        product1.setName("iPhone 15 Pro");
        product1.setDescription("最新款苹果手机，钛金属机身");
        product1.setPrice(new BigDecimal("7999.00"));
        product1.setStock(100);
        product1.setCategory("手机");
        product1.setStatus(true);

        Product product2 = new Product();
        product2.setName("MacBook Pro 14");
        product2.setDescription("专业级笔记本电脑，M3芯片");
        product2.setPrice(new BigDecimal("14999.00"));
        product2.setStock(50);
        product2.setCategory("电脑");
        product2.setStatus(true);

        Product product3 = new Product();
        product3.setName("AirPods Pro");
        product3.setDescription("主动降噪无线耳机");
        product3.setPrice(new BigDecimal("1999.00"));
        product3.setStock(200);
        product3.setCategory("耳机");
        product3.setStatus(true);

        Product product4 = new Product();
        product4.setName("iPad Air");
        product4.setDescription("轻薄平板电脑");
        product4.setPrice(new BigDecimal("4799.00"));
        product4.setStock(0);
        product4.setCategory("平板");
        product4.setStatus(false); // 下架

        createProduct(product1);
        createProduct(product2);
        createProduct(product3);
        createProduct(product4);
    }
}