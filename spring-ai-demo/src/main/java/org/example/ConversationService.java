package org.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ConversationService {
    
    private final ChatClient chatClient;

    // TODO 此处仅为简单模拟，实际应为数据库或其他存储方式
    private final Map<String, List<Message>> conversations = new ConcurrentHashMap<>();
    
    @Autowired
    public ConversationService(ChatClient chatClient) {
        this.chatClient = chatClient;
    }
    
    public String chat(String sessionId, String userMessage) {
        // 获取或创建会话历史
        List<Message> messages = conversations.computeIfAbsent(sessionId, k -> new ArrayList<>());
        
        // 添加用户消息
        messages.add(new UserMessage(userMessage));
        
        // 创建带有历史上下文的提示
        Prompt prompt = new Prompt(messages);
        
        // 调用AI
        String response = chatClient.prompt(prompt).call().content();
        
        // 保存AI回复
        messages.add(new AssistantMessage(response));
        
        // 管理会话长度，避免超出Token限制
        if (messages.size() > 10) {
            messages = messages.subList(messages.size() - 10, messages.size());
            conversations.put(sessionId, messages);
        }
        
        return response;
    }
    
    public void clearConversation(String sessionId) {
        conversations.remove(sessionId);
    }
}