package org.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class QaService {
    
    private final ChatClient chatClient;
    private final PromptTemplate promptTemplate;
    
    @Autowired
    public QaService(ChatClient chatClient) {
        this.chatClient = chatClient;
        // 创建一个提示模板，指导AI如何回答问题
        this.promptTemplate = new PromptTemplate("""
            你是一个智能问答助手，请简洁、准确地回答用户的问题。
            如果你不知道答案，请直接说不知道，不要编造信息。
            
            用户问题: {question}
            
            回答:
            """);
    }
    
    public String getAnswer(String question) {
        // 准备模板参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("question", question);
        
        // 创建提示
        Prompt prompt = promptTemplate.create(parameters);
        
        // 调用AI获取回答
        return chatClient.prompt(prompt).call().content();
    }
}