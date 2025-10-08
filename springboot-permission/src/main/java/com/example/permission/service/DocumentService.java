package com.example.permission.service;

import com.example.permission.entity.Document;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * 文档服务（业务逻辑层）
 */
@Slf4j
@Service
public class DocumentService {

    // 模拟数据存储
    private final Map<String, Document> documentStore = new ConcurrentHashMap<>();
    private final AtomicInteger idGenerator = new AtomicInteger(4);

    public DocumentService() {
        // 初始化测试数据
        initTestData();
    }

    private void initTestData() {
        createDocument(new Document("1", "研发部周报", "user1", "yfb", "report"));
        createDocument(new Document("2", "销售合同", "user2", "xsb", "contract"));
        createDocument(new Document("3", "公司公告", "admin", "xzb", "public"));
    }

    /**
     * 创建文档
     */
    public Document createDocument(Document doc) {
        if (doc.getId() == null || doc.getId().isEmpty()) {
            doc.setId(String.valueOf(idGenerator.getAndIncrement()));
        }
        doc.setCreateTime(LocalDateTime.now());
        doc.setUpdateTime(LocalDateTime.now());
        documentStore.put(doc.getId(), doc);
        log.info("创建文档：{}", doc.getId());
        return doc;
    }

    /**
     * 更新文档
     */
    public Document updateDocument(Document doc) {
        Document existing = documentStore.get(doc.getId());
        if (existing == null) {
            throw new RuntimeException("文档不存在：" + doc.getId());
        }

        existing.setTitle(doc.getTitle());
        existing.setContent(doc.getContent());
        existing.setType(doc.getType());
        existing.setUpdateTime(LocalDateTime.now());

        log.info("更新文档：{}", doc.getId());
        return existing;
    }

    /**
     * 删除文档
     */
    public void deleteDocument(String id) {
        Document removed = documentStore.remove(id);
        if (removed == null) {
            throw new RuntimeException("文档不存在：" + id);
        }
        log.info("删除文档：{}", id);
    }

    /**
     * 查询文档
     */
    public Document getDocument(String id) {
        Document doc = documentStore.get(id);
        if (doc == null) {
            throw new RuntimeException("文档不存在：" + id);
        }
        return doc;
    }

    /**
     * 查询所有文档
     */
    public List<Document> getAllDocuments() {
        return new ArrayList<>(documentStore.values());
    }

    /**
     * 按部门查询文档
     */
    public List<Document> getDocumentsByDept(String dept) {
        return documentStore.values().stream()
                .filter(doc -> dept.equals(doc.getDept()))
                .toList();
    }
}
