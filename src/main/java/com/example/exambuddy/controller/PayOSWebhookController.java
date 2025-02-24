package com.example.exambuddy.controller;


import com.example.exambuddy.model.PayOSWebhookRequest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.type.Webhook;
import vn.payos.type.WebhookData;
import java.util.Map;

@RestController
@RequestMapping("/payos_transfer_handler")
public class PayOSWebhookController {

  @PostMapping
  public ResponseEntity<String> handleWebhook(@RequestBody PayOSWebhookRequest request) {
    try {
      // Kiểm tra xem PayOS có báo thành công không
      if (request.isSuccess() ) {
        System.out.println(request.getData());
        // TODO: Xử lý đơn hàng tại đây (VD: cập nhật trạng thái đơn hàng trong DB)
        return ResponseEntity.ok("Success");
      } else {
        System.out.println("Thanh toán thất bại: " + request.getData().getDesc());
        return ResponseEntity.badRequest().body("Failed");
      }
    } catch (Exception e) {
      return ResponseEntity.status(500).body("Error: " + e.getMessage());
    }
  }
}
