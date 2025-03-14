package com.example.exambuddy.controller;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.RecordTopUser;
import com.example.exambuddy.service.ExamService;
import com.example.exambuddy.service.LeaderBoardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Controller
public class HomeController {
    @Autowired
    private ExamService examService;
    @Autowired
    private LeaderBoardService leaderBoardService;
    @GetMapping({"/*", "/home"})
    public String homePage(@RequestParam(defaultValue = "0") int page,
                           @RequestParam(defaultValue = "6") int size,
                           Model model) {
        try {
            long x = System.currentTimeMillis();

            List<Exam> exams = examService.getExamList(page, size);
              System.out.println("Exams in " + (System.currentTimeMillis() - x) + "ms");
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
    @ResponseBody
    @GetMapping("/top-score")
    public CompletableFuture<List<RecordTopUser>> getTopUserScore() {
        return leaderBoardService.getTopUserScore();
    }
    @ResponseBody
    @GetMapping("/top-contribute")
    public CompletableFuture<List<RecordTopUser>> getTopUserContribute() {
        return leaderBoardService.getTopUserScore();
    }
    @RequestMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    @GetMapping("/theory")
    public String index() {
        return "createPayment"; // Trả về tên file HTML (không cần đuôi .html)
    }

    @GetMapping("/createTheory")
    public String createTheory() {
        return "createTheory"; // Trả về tên file HTML (không cần đuôi .html)
    }
    @GetMapping("/viewTheory")
    public String viewTheory() {
        return "viewTheory"; // Trả về tên file HTML (không cần đuôi .html)
    }

    @GetMapping("/participant")
    public String participant() {
        return "examStatistics";}
    @GetMapping("/contact")
    public String contact() {
        return "contact";
    }
}
