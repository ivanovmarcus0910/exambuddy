package com.example.exambuddy.controller;


import com.example.exambuddy.config.PayOSConfig;
import com.example.exambuddy.model.PayOSWebhookData;
import com.example.exambuddy.model.PayOSWebhookRequest;
import com.example.exambuddy.service.LeaderBoardService;
import com.example.exambuddy.service.UserService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;

import java.util.Map;

@RestController
@RequestMapping("/payos_transfer_handler")
public class PayOSWebhookController {
    private final PayOS payOS = new PayOSConfig().payOS();
    @Autowired
    private UserService userService;
    @Autowired
    private LeaderBoardService leaderBoardService;
    @PostMapping
    public ResponseEntity<String> handleWebhook(@RequestBody PayOSWebhookRequest request) {
        try {
            PayOSWebhookData detail = request.getData();
            long orderCode = detail.getOrderCode();
            long amount = detail.getAmount();
            // Kiểm tra xem PayOS có báo thành công không
            if (request.isSuccess()) {

                String username = userService.updatePaymentStatus(orderCode, amount, "PAID");
                userService.changeCoinBalance(amount, username);
                leaderBoardService.updateUserContribute(username, amount/1000);
                return ResponseEntity.ok("Success");
            } else {
                String username = userService.updatePaymentStatus(orderCode, amount, "FAIL");
                return ResponseEntity.badRequest().body("Failed");
            }
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

}
