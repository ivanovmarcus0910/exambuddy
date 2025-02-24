package com.example.exambuddy.controller;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.service.ExamService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
public class HomeController {
    @Autowired
    private ExamService examService;
    @GetMapping("/home")
    public String homePage(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "6") int size,
                           Model model) {
        try {
            List<Exam> exams = examService.getExamList(page, size);
            model.addAttribute("exams", exams);
            model.addAttribute("currentPage", page);
            model.addAttribute("size", size);
            model.addAttribute("nextPage", page + 1);
            model.addAttribute("prevPage", page > 0 ? page - 1 : 0);
            return "home";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải danh sách đề thi: " + e.getMessage());
            return "error";
        }
    }
    @RequestMapping("/**")
    public String homePage1(
                           Model model) {
        try {
            List<Exam> exams = examService.getExamList(0, 6);
            model.addAttribute("exams", exams);
            model.addAttribute("currentPage", 0);
            model.addAttribute("size", 6);
            model.addAttribute("nextPage", 1);
            model.addAttribute("prevPage", 0);
            return "home";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải danh sách đề thi: " + e.getMessage());
            return "error";
        }
    }

    @RequestMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @GetMapping("/theory")
    public String index() {
        return "index"; // Trả về tên file HTML (không cần đuôi .html)
    }

    @GetMapping("/viewTheory")
    public String viewTheory() {
        return "viewTheory"; // Trả về tên file HTML (không cần đuôi .html)
    }
}
