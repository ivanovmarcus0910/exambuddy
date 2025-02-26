package com.example.exambuddy.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/payment")
public class PaymentController {

    @Value("${payos.api.key}")
    private String apiKey;

    @Value("${payos.api.endpoint}")
    private String payosEndpoint;

    @PostMapping("/create")
    public Map<String, Object> createPayment(@RequestParam double amount, @RequestParam String orderId) {
        RestTemplate restTemplate = new RestTemplate();

        // Tạo request body
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("amount", amount);
        requestBody.put("orderCode", orderId);
        requestBody.put("description", "Thanh toán đơn hàng " + orderId);
        requestBody.put("returnUrl", "http://localhost:8080/payment/success");
        requestBody.put("cancelUrl", "http://localhost:8080/payment/cancel");

        // Gửi request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        Map response = restTemplate.postForObject(payosEndpoint, request, Map.class);

        return response; // Trả về URL thanh toán
    }

    // Xử lý callback từ PayOS
    @PostMapping("/callback")
    public String paymentCallback(@RequestBody Map<String, Object> payload) {
        System.out.println("PAYOS CALLBACK DATA: " + payload);
        // Xử lý xác nhận thanh toán, cập nhật đơn hàng...
        return "OK";
    }

    @GetMapping("/success")
    public String success() {
        return "Thanh toán thành công!";
    }

    @GetMapping("/cancel")
    public String cancel() {
        return "Bạn đã hủy thanh toán.";
    }
}
