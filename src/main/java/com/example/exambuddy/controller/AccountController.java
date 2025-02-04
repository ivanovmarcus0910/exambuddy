package com.example.exambuddy.controller;

import com.example.exambuddy.service.FirebaseAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class AccountController {
    @Autowired
    private FirebaseAuthService authService;
    @RequestMapping("/logout")
    public String profilePage() {
        return "profile";
    }
}
