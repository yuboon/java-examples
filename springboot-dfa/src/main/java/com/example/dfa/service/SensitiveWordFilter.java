package com.example.dfa.service;

import com.example.dfa.entity.SensitiveWordResult;
import com.example.dfa.entity.TrieNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * DFA 敏感词过滤器
 * 基于 Trie 树实现的高效敏感词过滤算法
 */
@Slf4j
@Service
public class SensitiveWordFilter {

    private TrieNode root;
    private int minWordLength = 1;

    /**
     * 构造函数
     * @param sensitiveWords 敏感词列表
     */
    public SensitiveWordFilter(List<String> sensitiveWords) {
        this.root = buildTrie(sensitiveWords);
        this.minWordLength = sensitiveWords.stream()
                .mapToInt(String::length)
                .min()
                .orElse(1);

        log.info("DFA敏感词过滤器初始化完成，加载敏感词 {} 个", sensitiveWords.size());
    }

    /**
     * 默认构造函数，初始化基础敏感词库
     */
    public SensitiveWordFilter() {
        List<String> defaultWords = Arrays.asList(
            "apple", "app", "application", "apply", "orange"
        );
        this.root = buildTrie(defaultWords);
        this.minWordLength = defaultWords.stream()
                .mapToInt(String::length)
                .min()
                .orElse(1);

        log.info("DFA敏感词过滤器初始化完成，加载默认敏感词 {} 个", defaultWords.size());
    }

    /**
     * 构建 Trie 树
     */
    private TrieNode buildTrie(List<String> words) {
        TrieNode root = new TrieNode();
        for (String word : words) {
            if (word == null || word.trim().isEmpty()) {
                continue;
            }

            word = word.trim().toLowerCase();
            TrieNode node = root;

            for (char c : word.toCharArray()) {
                node = node.addChild(c);
            }

            node.setEnd(true);
            node.setKeyword(word);
        }
        return root;
    }

    /**
     * 检查是否包含敏感词 - 核心 DFA 匹配算法
     */
    public boolean containsSensitiveWord(String text) {
        if (text == null || text.length() < minWordLength) {
            return false;
        }

        char[] chars = text.toLowerCase().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            if (dfaMatch(chars, i)) {
                return true;
            }
        }
        return false;
    }

    /**
     * DFA 状态转移匹配
     */
    private boolean dfaMatch(char[] chars, int start) {
        TrieNode node = root;

        for (int i = start; i < chars.length; i++) {
            char c = chars[i];

            if (!node.hasChild(c)) {
                break; // 状态转移失败
            }

            node = node.getChild(c);

            if (node.isEnd()) {
                return true; // 到达接受状态
            }
        }
        return false;
    }

    /**
     * 查找并替换敏感词
     */
    public String filter(String text, String replacement) {
        if (text == null || text.length() < minWordLength) {
            return text;
        }

        List<SensitiveWordResult> words = findAllWords(text);
        if (words.isEmpty()) {
            return text;
        }

        // 从后往前替换，避免索引变化问题
        StringBuilder result = new StringBuilder(text);
        for (int i = words.size() - 1; i >= 0; i--) {
            SensitiveWordResult word = words.get(i);
            String stars = String.valueOf(replacement != null ? replacement : "*")
                    .repeat(word.getEnd() - word.getStart() + 1);
            result.replace(word.getStart(), word.getEnd() + 1, stars);
        }
        return result.toString();
    }

    /**
     * 查找所有敏感词
     */
    public List<SensitiveWordResult> findAllWords(String text) {
        List<SensitiveWordResult> results = new ArrayList<>();

        if (text == null || text.length() < minWordLength) {
            return results;
        }

        char[] chars = text.toLowerCase().toCharArray();
        for (int i = 0; i < chars.length; i++) {
            TrieNode node = root;
            int j = i;

            while (j < chars.length && node.hasChild(chars[j])) {
                node = node.getChild(chars[j]);
                j++;

                if (node.isEnd()) {
                    // 获取原始文本中的敏感词
                    String originalWord = text.substring(i, j);
                    results.add(new SensitiveWordResult(originalWord, i, j - 1));
                }
            }
        }
        return results;
    }

    /**
     * 重新加载敏感词库
     */
    public void reloadWords(List<String> words) {
        this.root = buildTrie(words);
        this.minWordLength = words.stream()
                .mapToInt(String::length)
                .min()
                .orElse(1);

        log.info("敏感词库重新加载完成，当前词数：{}", words.size());
    }

    /**
     * 添加单个敏感词
     */
    public void addWord(String word) {
        if (word == null || word.trim().isEmpty()) {
            return;
        }

        word = word.trim().toLowerCase();
        TrieNode node = root;

        for (char c : word.toCharArray()) {
            node = node.addChild(c);
        }

        node.setEnd(true);
        node.setKeyword(word);

        // 更新最小词长度
        this.minWordLength = Math.min(this.minWordLength, word.length());
    }
}