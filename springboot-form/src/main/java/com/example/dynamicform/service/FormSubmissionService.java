package com.example.dynamicform.service;

import com.example.dynamicform.model.FormSubmission;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * 表单提交管理服务（基于内存Map存储）
 */
@Service
@Slf4j
public class FormSubmissionService {

    // 使用内存Map存储提交数据
    private final Map<String, List<FormSubmission>> submissionStore = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);

    /**
     * 保存表单提交数据
     */
    public FormSubmission saveSubmission(String schemaId, String formData, String submitterId) {
        FormSubmission submission = FormSubmission.builder()
            .id(idGenerator.getAndIncrement())
            .schemaId(schemaId)
            .formData(formData)
            .submitterId(submitterId != null ? submitterId : "anonymous")
            .status("pending")
            .submittedAt(LocalDateTime.now())
            .build();

        // 保存到存储
        submissionStore.computeIfAbsent(schemaId, k -> new ArrayList<>()).add(submission);

        log.info("Saved submission for schema: {}, submission ID: {}", schemaId, submission.getId());
        return submission;
    }

    /**
     * 获取表单的所有提交数据
     */
    public List<FormSubmission> getSubmissionsBySchema(String schemaId) {
        return submissionStore.getOrDefault(schemaId, new ArrayList<>());
    }

    /**
     * 根据ID获取提交数据
     */
    public FormSubmission getSubmissionById(Long id) {
        for (List<FormSubmission> submissions : submissionStore.values()) {
            for (FormSubmission submission : submissions) {
                if (submission.getId().equals(id)) {
                    return submission;
                }
            }
        }
        return null;
    }

    /**
     * 获取所有提交数据
     */
    public List<FormSubmission> getAllSubmissions() {
        List<FormSubmission> allSubmissions = new ArrayList<>();
        for (List<FormSubmission> submissions : submissionStore.values()) {
            allSubmissions.addAll(submissions);
        }
        return allSubmissions;
    }

    /**
     * 删除提交数据
     */
    public boolean deleteSubmission(Long id) {
        for (Map.Entry<String, List<FormSubmission>> entry : submissionStore.entrySet()) {
            List<FormSubmission> submissions = entry.getValue();
            boolean removed = submissions.removeIf(submission -> submission.getId().equals(id));
            if (removed) {
                log.info("Deleted submission: {}", id);
                return true;
            }
        }
        return false;
    }

    /**
     * 更新提交状态
     */
    public boolean updateSubmissionStatus(Long id, String status) {
        FormSubmission submission = getSubmissionById(id);
        if (submission != null) {
            submission.setStatus(status);
            log.info("Updated submission {} status to: {}", id, status);
            return true;
        }
        return false;
    }

    /**
     * 获取统计信息
     */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new HashMap<>();

        int totalSubmissions = getAllSubmissions().size();
        int totalSchemas = submissionStore.size();

        Map<String, Long> statusCount = new HashMap<>();
        Map<String, Integer> schemaCount = new HashMap<>();

        for (FormSubmission submission : getAllSubmissions()) {
            statusCount.put(submission.getStatus(),
                statusCount.getOrDefault(submission.getStatus(), 0L) + 1);
        }

        for (Map.Entry<String, List<FormSubmission>> entry : submissionStore.entrySet()) {
            schemaCount.put(entry.getKey(), entry.getValue().size());
        }

        stats.put("totalSubmissions", totalSubmissions);
        stats.put("totalSchemas", totalSchemas);
        stats.put("statusCount", statusCount);
        stats.put("schemaCount", schemaCount);

        return stats;
    }
}