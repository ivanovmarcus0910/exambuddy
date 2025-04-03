package com.example.exambuddy.service;

import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service
public class AIService {
    private static final String API_URL = "https://api.groq.com/openai/v1/chat/completions";
    private static final String API_KEY = "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"; // Thay bằng API Key của bạn
    private final RestTemplate restTemplate = new RestTemplate();

    public String getAIExplanation(String question) {
        try {
            // Tạo headers
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + API_KEY);

            // Chuẩn bị dữ liệu gửi đi theo đúng format API của Groq
            Map<String, Object> requestData = new HashMap<>();
            requestData.put("model", "llama-3.3-70b-versatile"); // Sử dụng model chính xác

            // Tạo danh sách messages theo chuẩn của Groq API
            List<Map<String, String>> messages = new ArrayList<>();
            messages.add(Map.of("role", "user", "content", question));
            requestData.put("messages", messages);

            // Đóng gói request
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestData, headers);

            // Gửi yêu cầu POST
            ResponseEntity<Map> response = restTemplate.exchange(API_URL, HttpMethod.POST, request, Map.class);

            // Kiểm tra phản hồi
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                // Trích xuất nội dung phản hồi từ API
                List<Map<String, Object>> choices = (List<Map<String, Object>>) response.getBody().get("choices");
                if (choices != null && !choices.isEmpty()) {
                    Map<String, Object> firstChoice = choices.get(0);
                    Map<String, Object> message = (Map<String, Object>) firstChoice.get("message");
                    return message.get("content").toString(); // Trả về nội dung phản hồi từ AI
                }
            }
            return "Không có phản hồi từ AI.";
        } catch (Exception e) {
            return "Lỗi khi gọi AI: " + e.getMessage();
        }
    }
}
