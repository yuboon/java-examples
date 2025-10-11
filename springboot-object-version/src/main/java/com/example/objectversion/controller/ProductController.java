package com.example.objectversion.controller;

import com.example.objectversion.dto.ProductRequest;
import com.example.objectversion.model.AuditLog;
import com.example.objectversion.model.Product;
import com.example.objectversion.service.ProductService;
import com.example.objectversion.aspect.AuditAspect;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collection;
import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;
    private final AuditAspect auditAspect;

    public ProductController(ProductService productService, AuditAspect auditAspect) {
        this.productService = productService;
        this.auditAspect = auditAspect;
    }

    @GetMapping
    public Collection<Product> findAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> findById(@PathVariable String id) {
        return productService.findById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> upsert(@PathVariable String id,
                                          @Valid @RequestBody ProductRequest request,
                                          @RequestHeader(name = "X-User", required = false) String actor) {
        boolean existed = productService.findById(id).isPresent();
        Product result = productService.upsert(id, request, normalizeActor(actor));
        HttpStatus status = existed ? HttpStatus.OK : HttpStatus.CREATED;
        return new ResponseEntity<>(result, status);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable String id,
                                       @RequestHeader(name = "X-User", required = false) String actor) {
        boolean removed = productService.delete(id, normalizeActor(actor));
        return removed ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    @GetMapping("/{id}/audits")
    public List<AuditLog> findAudits(@PathVariable String id) {
        return auditAspect.findAuditByEntityId(id);
    }

    @GetMapping("/audits")
    public List<AuditLog> findAllAudits() {
        return auditAspect.findAllAudits();
    }

    private String normalizeActor(String actor) {
        return actor == null || actor.isBlank() ? "anonymous" : actor.trim();
    }
}
