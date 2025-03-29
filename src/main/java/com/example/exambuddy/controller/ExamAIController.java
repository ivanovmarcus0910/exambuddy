package com.example.exambuddy.controller;

import com.example.exambuddy.service.AIService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ExamAIController {

    private final AIService groqService;

    public ExamAIController(AIService groqService) {
        this.groqService = groqService;
    }

    @GetMapping("/explain")
    public ResponseEntity<String> getExplanation(@RequestParam String question) {
        String explanation = groqService.getAIExplanation(question);
        return ResponseEntity.ok(explanation);
    }
}
