package org.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.util.List;

@SpringBootApplication
public class QaApplication {

    public static void main(String[] args) {
        SpringApplication.run(QaApplication.class, args);
    }

    @Bean
    public ChatClient chatClient(OpenAiChatModel model){
        return ChatClient
                .builder(model)
                .build();
    }

    @Bean
    public VectorStore vectorStore(EmbeddingModel embeddingModel) {
        VectorStore vectorStore = SimpleVectorStore.builder(embeddingModel).build();

        // 构建测试数据
        List<Document> documents =
                List.of(new Document("Hello Spring AI"),
                        new Document("Hello Spring Boot"));
        // 添加到向量数据库
        vectorStore.add(documents);

        return vectorStore;
    }

}