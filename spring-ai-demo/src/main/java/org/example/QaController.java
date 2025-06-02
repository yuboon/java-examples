package org.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/qa")
public class QaController {
    
    private final QaService qaService;

    private final ConversationService conversationService;

    private final KnowledgeBaseQaService knowledgeBaseQaService;
    
    @Autowired
    public QaController(QaService qaService,
                        ConversationService conversationService,
                        KnowledgeBaseQaService knowledgeBaseQaService
    ) {
        this.qaService = qaService;
        this.conversationService = conversationService;
        this.knowledgeBaseQaService = knowledgeBaseQaService;
    }
    
    @PostMapping("/ask")
    public Map<String, String> askQuestion(@RequestBody QuestionRequest request) {
        String answer = qaService.getAnswer(request.getQuestion());
        
        Map<String, String> response = new HashMap<>();
        response.put("question", request.getQuestion());
        response.put("answer", answer);
        
        return response;
    }

    @PostMapping("/ask-session")
    public Map<String, String> askSession(@RequestBody QuestionRequest request) {
        String answer = conversationService.chat(request.getSessionId(),request.getQuestion());

        Map<String, String> response = new HashMap<>();
        response.put("question", request.getQuestion());
        response.put("answer", answer);

        return response;
    }

    @PostMapping("/ask-knowledge")
    public Map<String, String> askKnowledge(@RequestBody QuestionRequest request) {
        String answer = knowledgeBaseQaService.getAnswerWithKnowledgeBase(request.getQuestion());

        Map<String, String> response = new HashMap<>();
        response.put("question", request.getQuestion());
        response.put("answer", answer);

        return response;
    }

}