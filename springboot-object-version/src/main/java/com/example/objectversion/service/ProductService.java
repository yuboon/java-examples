package com.example.objectversion.service;

import com.example.objectversion.annotation.Audit;
import com.example.objectversion.dto.ProductRequest;
import com.example.objectversion.model.Product;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class ProductService {

    private final Map<String, Product> products = new ConcurrentHashMap<>();

    public Collection<Product> findAll() {
        return products.values();
    }

    public Optional<Product> findById(String id) {
        return Optional.ofNullable(products.get(id));
    }

    @Audit(
            action = Audit.ActionType.CREATE,
            idParam = "id",
            actorParam = "actor",
            entityIndex = 1
    )
    public Product create(String id, ProductRequest request, String actor) {
        Product newProduct = new Product(
                id,
                request.name(),
                request.price(),
                request.description()
        );
        return products.put(id, newProduct);
    }

    @Audit(
            action = Audit.ActionType.UPDATE,
            idParam = "id",
            actorParam = "actor",
            entityIndex = 1
    )
    public Product update(String id, ProductRequest request, String actor) {
        Product existingProduct = products.get(id);
        if (existingProduct == null) {
            throw new IllegalArgumentException("产品不存在: " + id);
        }

        Product updatedProduct = new Product(
                id,
                request.name(),
                request.price(),
                request.description()
        );
        return products.put(id, updatedProduct);
    }

    @Audit(
            action = Audit.ActionType.DELETE,
            idParam = "id",
            actorParam = "actor",
            entityIndex = -1  // 删除操作不需要实体对象
    )
    public boolean delete(String id, String actor) {
        return products.remove(id) != null;
    }

    /**
     * 通用的 upsert 方法，支持创建和更新
     */
    @Audit(
            idParam = "id",           // ID来自第一个参数
            actorParam = "actor",     // 操作人来自第三个参数
            entityIndex = 1           // 实体对象是ProductRequest
    )
    public Product upsert(String id, ProductRequest request, String actor) {
        Product newProduct = new Product(
                id,
                request.name(),
                request.price(),
                request.description()
        );
        return products.put(id, newProduct);
    }
}
