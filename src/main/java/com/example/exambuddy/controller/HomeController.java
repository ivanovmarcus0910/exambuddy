package com.example.exambuddy.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class HomeController {
    @GetMapping("/home")
    public String homePage() {
        return "home";
    }
    @RequestMapping("*")
    public String handleAllRequests() {
        return "home"; // Trả về view "home" cho tất cả các URL không khớp
    }
    @RequestMapping("/profile")
    public String profilePage() {
        return "profile";
    }
    @RequestMapping("/signup")
    public String signupPage() {
        return "signup";
    }

}
