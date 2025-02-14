package com.example.exambuddy.controller;

import jakarta.servlet.http.HttpServletRequest;
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
    public String handleAllRequests(HttpServletRequest request) {
        System.out.println("Request không khớp: " + request.getRequestURI());

        return "home"; // Trả về view "home" cho tất cả các URL không khớp
    }

    @RequestMapping("/signup")
    public String signupPage() {
        return "signup";
    }

}
