package com.example.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

/**
 * 流式聊天服务
 * 模拟大模型流式生成响应
 */
@Slf4j
@Service
public class StreamChatService {

    /**
     * 模拟大模型流式生成响应
     *
     * @param prompt 用户问题
     * @return 按字符/词汇流式输出的响应
     */
    public Flux<String> streamResponse(String prompt) {
        log.info("收到用户提问: {}", prompt);

        // 模拟大模型生成的回复内容
        String response = mockLLMResponse(prompt);

        // 将文本拆分成小块，使用响应式延迟模拟打字效果
        int chunkSize = 2; // 每次发送 2 个字符

        return Flux.fromArray(splitIntoChunks(response, chunkSize))
                .delayElements(Duration.ofMillis(30)) // 每 30ms 发送一个块
                .doOnComplete(() -> log.info("流式响应完成"));
    }

    /**
     * 将字符串拆分成固定大小的块
     */
    private String[] splitIntoChunks(String text, int chunkSize) {
        int length = (text.length() + chunkSize - 1) / chunkSize;
        String[] chunks = new String[length];
        for (int i = 0; i < length; i++) {
            int start = i * chunkSize;
            int end = Math.min(start + chunkSize, text.length());
            chunks[i] = text.substring(start, end);
        }
        return chunks;
    }

    /**
     * 模拟大模型生成内容
     * 实际项目可接入 OpenAI/通义千问等 API
     *
     * @param prompt 用户问题
     * @return 模拟的回复内容
     */
    private String mockLLMResponse(String prompt) {
        return """
                【Spring Boot 流式响应】
                您的问题是：%s

                这是一个模拟大模型流式输出的示例。
                在实际应用中，你可以：
                1. 接入 OpenAI API 使用 GPT-4
                2. 接入阿里云通义千问 API
                3. 接入本地部署的大模型

                流式响应的核心是：
                - 使用 Spring WebFlux 的 Flux
                - 返回 text/event-stream 格式
                - 前端使用 EventSource 或 fetch 接收

                这样就能实现像 ChatGPT 一样的丝滑体验！
                """.formatted(prompt);
    }
}
