package org.example;

import lombok.Data;

@Data
public class QuestionRequest {
    private String question;
    private String sessionId;
}