package com.example.dfa.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 敏感词检测结果
 */
@Data
@NoArgsConstructor
public class SensitiveWordResult {
    /**
     * 敏感词内容
     */
    private String word;

    /**
     * 起始位置
     */
    private int start;

    /**
     * 结束位置
     */
    private int end;

    public SensitiveWordResult(String word, int start, int end) {
        this.word = word;
        this.start = start;
        this.end = end;
    }
}