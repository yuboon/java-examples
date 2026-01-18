# 像 ChatGPT 一样丝滑：Spring Boot 如何实现大模型流式（Streaming）响应？

## 一、为什么需要流式响应？

同样的 HTTP 请求，为什么像 ChatGPT 这类模型的回答能像打字机一样逐字输出，而我们平时写的接口却要等全部处理完才返回？

问题的核心在于 **响应模式**：

| 传统模式 | 流式模式 |
|---------|---------|
| 服务器处理完成 → 一次性返回 | 生成一部分 → 立即推送 |
| 客户端等待总时长 = 服务器处理时间 | 客户端首字等待时间通常很短 |
| 适合快速查询 | 适合耗时生成 |

对于大模型这种 **生成式 AI**，一个响应可能需要几秒甚至几十秒。如果用传统模式，用户体验就是：

```
提问 → (10秒空白) → 答案全部出现
```

而流式响应的体验是：

```
提问 → 0.1秒后 → "我" → "认" → "为" → ... → 逐字呈现
```

实现这种效果有多种技术方案，本文将介绍基于 Spring Boot WebFlux + SSE 的实现方式。

---

## 二、核心技术选型

实现流式响应主要有以下几种方案：

| 方案 | 优点 | 缺点 | 适用场景 |
|------|------|------|----------|
| **SSE** | 单向推送、HTTP协议、实现简单 | 不支持双向通信 | 服务端主动推送 |
| **WebSocket** | 双向通信、实时性强 | 实现复杂、需要额外协议 | 聊天、游戏 |
| **长轮询** | 兼容性好 | 资源消耗大 | 低频数据更新 |

**本文选择 SSE 方案**，原因如下：
- Spring Boot 原生支持 `ResponseEntity<Flux<String>>`
- 基于标准 HTTP，无需额外协议协商
- 代码简洁，易于理解和维护

---

## 三、项目依赖配置

### 3.1 Maven 依赖

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
        <relativePath/>
    </parent>

    <groupId>com.example</groupId>
    <artifactId>springboot-chat-stream</artifactId>
    <version>1.0.0</version>

    <properties>
        <java.version>17</java.version>
    </properties>

    <dependencies>
        <!-- Spring Boot WebFlux：响应式编程支持 -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-webflux</artifactId>
        </dependency>

        <!-- Lombok：简化代码 -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

### 3.2 关键依赖说明

- **spring-boot-starter-webflux**：提供响应式 Web 支持，核心是 Reactor 的 `Flux` 类型
- **Reactor**：响应式编程库，`Flux<T>` 表示 0-N 个元素的异步序列

---

## 四、核心代码实现

### 4.1 Controller 层：流式响应入口

```java
package com.example.chat.controller;

import com.example.chat.service.StreamChatService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@CrossOrigin(origins = "*") // 开发环境允许跨域
public class StreamChatController {

    private final StreamChatService chatService;

    /**
     * 流式聊天接口
     * @param prompt 用户输入的问题
     * @return 流式响应，text/event-stream 格式
     */
    @GetMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public ResponseEntity<Flux<String>> streamChat(@RequestParam String prompt) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-cache")
                .header("Connection", "keep-alive")
                .body(chatService.streamResponse(prompt));
    }
}
```

**关键点解析：**

1. `produces = MediaType.TEXT_EVENT_STREAM_VALUE`：声明返回 SSE 格式
2. `Flux<String>`：响应式流，可以发送多个数据块
3. `Cache-Control: no-cache`：禁用缓存，确保实时推送

### 4.2 Service 层：模拟大模型流式生成

```java
package com.example.chat.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;

@Slf4j
@Service
public class StreamChatService {

    /**
     * 模拟大模型流式生成响应
     * @param prompt 用户问题
     * @return 按字符/词汇流式输出的响应
     */
    public Flux<String> streamResponse(String prompt) {
        log.info("收到用户提问: {}", prompt);

        // 模拟大模型生成的回复内容
        String response = mockLLMResponse(prompt);

        // 将响应拆分为字符流，每 50ms 发送一个字符
        return Flux.fromArray(response.split(""))
                .delayElements(Duration.ofMillis(50))
                .doOnNext(chunk -> log.debug("发送数据块: {}", chunk))
                .doOnComplete(() -> log.info("流式响应完成"))
                .doOnError(e -> log.error("流式响应异常", e));
    }

    /**
     * 模拟大模型生成内容（实际项目可接入 OpenAI/通义千问等）
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
```

**核心逻辑：**

1. `Flux.fromArray(response.split(""))`：将字符串拆分为字符数组转为流
2. `.delayElements(Duration.ofMillis(50))`：每个字符延迟 50ms 发送
3. `.doOnNext()/.doOnComplete()/.doOnError()`：生命周期钩子，用于日志记录

### 4.3 接入真实大模型 API（扩展）

```java
// 接入 OpenAI Streaming API 的示例（伪代码）
public Flux<String> streamOpenAI(String prompt) {
    WebClient webClient = WebClient.builder()
            .baseUrl("https://api.openai.com/v1")
            .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer YOUR_API_KEY")
            .build();

    return webClient.post()
            .uri("/chat/completions")
            .bodyValue(Map.of(
                "model", "gpt-4",
                "messages", List.of(Map.of("role", "user", "content", prompt)),
                "stream", true
            ))
            .retrieve()
            .bodyToFlux(String.class)
            .map(this::extractContentFromSSE); // 解析 SSE 格式提取 content
}
```

### 4.4 启动类

```java
package com.example.chat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ChatStreamApplication {
    public static void main(String[] args) {
        SpringApplication.run(ChatStreamApplication.class, args);
    }
}
```

### 4.5 配置文件

```yaml
server:
  port: 8080

spring:
  application:
    name: chat-stream-demo

# 日志配置
logging:
  level:
    com.example.chat: DEBUG
```

---

## 五、前端对接示例

### 5.1 使用 EventSource 接收流

```html
<!DOCTYPE html>
<html>
<head>
    <meta charset="UTF-8">
    <title>Spring Boot 流式响应示例</title>
    <style>
        body { font-family: Arial; max-width: 800px; margin: 50px auto; padding: 20px; }
        #output { border: 1px solid #ddd; padding: 20px; min-height: 100px; background: #f9f9f9; }
        input { width: 70%; padding: 10px; }
        button { padding: 10px 20px; cursor: pointer; }
    </style>
</head>
<body>
    <h1>Spring Boot 流式聊天</h1>
    <input type="text" id="prompt" placeholder="输入问题...">
    <button onclick="sendQuestion()">发送</button>
    <div id="output"></div>

    <script>
        function sendQuestion() {
            const prompt = document.getElementById('prompt').value;
            const output = document.getElementById('output');
            output.innerHTML = '等待响应...';

            // 使用 EventSource 接收 SSE 流
            const eventSource = new EventSource(
                `/api/chat/stream?prompt=${encodeURIComponent(prompt)}`
            );

            eventSource.onmessage = (event) => {
                output.innerHTML += event.data;
            };

            eventSource.onerror = () => {
                eventSource.close();
                output.innerHTML += '<br><br>[连接关闭]';
            };

            // 30秒后自动关闭（演示用）
            setTimeout(() => eventSource.close(), 30000);
        }
    </script>
</body>
</html>
```

### 5.2 使用 Fetch API（推荐）

```javascript
async function streamChat(prompt) {
    const response = await fetch(`/api/chat/stream?prompt=${encodeURIComponent(prompt)}`);
    const reader = response.body.getReader();
    const decoder = new TextDecoder();

    while (true) {
        const { done, value } = await reader.read();
        if (done) break;

        const chunk = decoder.decode(value);
        console.log('收到数据:', chunk);
        // 更新 UI
    }
}
```

---

## 六、运行效果

启动项目后，访问 `http://localhost:8080`（需添加静态页面支持），输入问题，你会看到：

```
【Spring Boot 流式响应】
您的问题是：如何学习 Spring Boot？

这是一个模拟大模型流式输出的示例。
...
```

文字像打字机一样逐字出现，体验丝滑！


---

## 七、总结

本文介绍了如何使用 Spring Boot WebFlux 实现 SSE 流式响应。核心是通过 `Flux<String>` + `TEXT_EVENT_STREAM_VALUE` 将数据分块推送，配合前端 `EventSource` 实现逐字显示效果。相比传统一次性返回，流式响应能显著降低用户等待感知，特别适合大模型对话等耗时生成场景。
