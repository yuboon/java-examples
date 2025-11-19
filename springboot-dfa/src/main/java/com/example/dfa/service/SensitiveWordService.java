package com.example.dfa.service;

import com.example.dfa.entity.SensitiveWordResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

/**
 * 敏感词服务
 * 提供敏感词过滤相关的业务逻辑
 */
@Slf4j
@Service
public class SensitiveWordService {

    @Autowired
    private SensitiveWordFilter sensitiveWordFilter;

    /**
     * 检查文本是否包含敏感词
     */
    public boolean containsSensitiveWord(String text) {
        return sensitiveWordFilter.containsSensitiveWord(text);
    }

    /**
     * 过滤文本中的敏感词
     */
    public String filterText(String text, String replacement) {
        return sensitiveWordFilter.filter(text, replacement);
    }

    /**
     * 查找文本中的所有敏感词
     */
    public List<SensitiveWordResult> findAllSensitiveWords(String text) {
        return sensitiveWordFilter.findAllWords(text);
    }

    /**
     * 添加敏感词到词库
     */
    public void addSensitiveWord(String word) {
        sensitiveWordFilter.addWord(word);
        log.info("添加敏感词到词库: {}", word);
    }

    /**
     * 批量添加敏感词
     */
    public void addSensitiveWords(List<String> words) {
        for (String word : words) {
            addSensitiveWord(word);
        }
    }

    /**
     * 重新加载敏感词库
     */
    public void reloadSensitiveWords(List<String> words) {
        sensitiveWordFilter.reloadWords(words);
        log.info("重新加载敏感词库，共 {} 个词", words.size());
    }

    /**
     * 获取完整的过滤结果
     */
    public FilterResult getFilterResult(String text, String replacement) {
        boolean hasSensitive = containsSensitiveWord(text);
        String filteredText = filterText(text, replacement);
        List<SensitiveWordResult> sensitiveWords = findAllSensitiveWords(text);

        return new FilterResult(
                text,
                filteredText,
                hasSensitive,
                sensitiveWords,
                sensitiveWords.size()
        );
    }

    /**
     * 过滤结果封装类
     */
    public static class FilterResult {
        private String originalText;      // 原始文本
        private String filteredText;      // 过滤后文本
        private boolean hasSensitive;     // 是否包含敏感词
        private List<SensitiveWordResult> sensitiveWords; // 敏感词列表
        private int sensitiveWordCount;   // 敏感词数量

        public FilterResult() {}

        public FilterResult(String originalText, String filteredText,
                          boolean hasSensitive, List<SensitiveWordResult> sensitiveWords,
                          int sensitiveWordCount) {
            this.originalText = originalText;
            this.filteredText = filteredText;
            this.hasSensitive = hasSensitive;
            this.sensitiveWords = sensitiveWords;
            this.sensitiveWordCount = sensitiveWordCount;
        }

        // Getters and Setters
        public String getOriginalText() {
            return originalText;
        }

        public void setOriginalText(String originalText) {
            this.originalText = originalText;
        }

        public String getFilteredText() {
            return filteredText;
        }

        public void setFilteredText(String filteredText) {
            this.filteredText = filteredText;
        }

        public boolean isHasSensitive() {
            return hasSensitive;
        }

        public void setHasSensitive(boolean hasSensitive) {
            this.hasSensitive = hasSensitive;
        }

        public List<SensitiveWordResult> getSensitiveWords() {
            return sensitiveWords;
        }

        public void setSensitiveWords(List<SensitiveWordResult> sensitiveWords) {
            this.sensitiveWords = sensitiveWords;
        }

        public int getSensitiveWordCount() {
            return sensitiveWordCount;
        }

        public void setSensitiveWordCount(int sensitiveWordCount) {
            this.sensitiveWordCount = sensitiveWordCount;
        }
    }
}