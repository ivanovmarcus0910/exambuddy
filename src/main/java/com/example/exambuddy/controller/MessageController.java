package com.example.exambuddy.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class MessageController {
    @ResponseBody
    @PostMapping("/message")
    public String ReceiveMessage(@RequestParam String message, HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");

        return message;
    }
}
