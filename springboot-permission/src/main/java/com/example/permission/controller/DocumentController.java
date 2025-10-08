package com.example.permission.controller;

import com.example.permission.annotation.CheckPermission;
import com.example.permission.common.AccessDeniedException;
import com.example.permission.common.Result;
import com.example.permission.entity.Document;
import com.example.permission.service.DocumentService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 文档管理控制器
 */
@Slf4j
@RestController
@RequestMapping("/api/documents")
@CrossOrigin(origins = "*")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * 查询所有文档（无权限限制）
     */
    @GetMapping
    public Result<List<Document>> list() {
        return Result.success(documentService.getAllDocuments());
    }

    /**
     * 查询单个文档（需要 read 权限）
     */
    @CheckPermission(action = "read")
    @GetMapping("/{id}")
    public Result<Document> get(@PathVariable String id) {
        Document doc = documentService.getDocument(id);
        return Result.success(doc);
    }

    /**
     * 创建文档（无权限限制）
     */
    @PostMapping
    public Result<Document> create(@RequestBody Document doc) {
        Document created = documentService.createDocument(doc);
        return Result.success(created);
    }

    /**
     * 更新文档（需要 edit 权限）
     */
    @CheckPermission(action = "edit")
    @PutMapping("/{id}")
    public Result<Document> update(@PathVariable String id, @RequestBody Document doc) {
        doc.setId(id);
        Document updated = documentService.updateDocument(doc);
        return Result.success(updated);
    }

    /**
     * 删除文档（需要 delete 权限）
     */
    @CheckPermission(action = "delete")
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        Document doc = documentService.getDocument(id);
        documentService.deleteDocument(id);
        return Result.success();
    }

    /**
     * 全局异常处理
     */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("权限拒绝：{}", e.getMessage());
        return Result.error(403, e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public Result<Void> handleException(Exception e) {
        log.error("服务异常", e);
        return Result.error(e.getMessage());
    }
}
