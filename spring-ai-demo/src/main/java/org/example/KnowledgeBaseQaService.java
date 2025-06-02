package org.example;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.Embedding;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class KnowledgeBaseQaService {
    
    private final ChatClient chatClient;
    private final VectorStore vectorStore;
    
    @Autowired
    public KnowledgeBaseQaService(
            ChatClient chatClient, 
            VectorStore vectorStore) {
        this.chatClient = chatClient;
        this.vectorStore = vectorStore;
    }
    
    public String getAnswerWithKnowledgeBase(String question) {
        // 在知识库中搜索相关文档
        List<Document> relevantDocs = vectorStore.similaritySearch(question);
        
        // 构建上下文
        StringBuilder context = new StringBuilder();
        for (Document doc : relevantDocs) {
            context.append(doc.getText()).append("\n\n");
        }
        
        // 创建提示模板
        PromptTemplate promptTemplate = new PromptTemplate("""
            你是一个智能问答助手。请根据以下提供的信息回答用户问题。
            如果无法从提供的信息中找到答案，请基于你的知识谨慎回答，并明确指出这是你的一般性了解。
            
            参考信息:
            {context}
            
            用户问题: {question}
            
            回答:
            """);
        
        // 准备参数
        Map<String, Object> parameters = new HashMap<>();
        parameters.put("context", context.toString());
        parameters.put("question", question);
        
        // 创建提示并调用AI
        Prompt prompt = promptTemplate.create(parameters);
        return chatClient.prompt(prompt).call().content();
    }
}