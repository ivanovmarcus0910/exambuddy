package com.example.exambuddy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class PolicyController {

    @GetMapping("/policy")
    public String policyPage() {
        return "policy"; // trả về view policy.html từ thư mục templates
    }
    @GetMapping("/privacy")
    public String privacyPage() {
        return "privacy"; // Trả về view privacy.html nằm trong thư mục templates
    }
}
