package com.example.exambuddy.controller;

import com.example.exambuddy.model.ReportRequest;
import com.example.exambuddy.service.MessageService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MessageController {
    @Autowired
    private MessageService messageService;

    @ResponseBody
    @PostMapping("/message")
    public String ReceiveMessage(@RequestParam String message, HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");

        return message;
    }

    @ResponseBody
    @PostMapping("/reportPost")
    public ResponseEntity<String> reportPost(@RequestBody ReportRequest request, HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để báo cáo bài viết.");
        }

        request.setReporter(username); // Lưu người báo cáo vào request
        messageService.saveReport(request);

        return ResponseEntity.ok("Báo cáo đã được gửi thành công");
    }
}
