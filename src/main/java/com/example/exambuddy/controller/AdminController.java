package com.example.exambuddy.controller;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.Post;
import com.example.exambuddy.model.User;
import com.example.exambuddy.service.ExamService;
import com.example.exambuddy.service.FirebaseAuthService;
import com.example.exambuddy.service.PostService;
import com.example.exambuddy.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/adminDashboard")
public class AdminController {
    @Autowired
    private FirebaseAuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExamService examService;


    // ✅ Chỉ Admin mới có thể truy cập trang này
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        System.out.println("📌 Session hiện tại: " + loggedInUser);

        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            System.out.println("❌ Không có user trong session hoặc không phải admin. Chuyển về login.");
            return "redirect:/login";
        }

        // Lấy danh sách người dùng
        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);

        // Lấy danh sách đề thi
        List<Exam> exams = examService.getAllExams();
        model.addAttribute("exams", exams);

        // Lấy danh sách bài đăng
        List<Post> posts = PostService.getPostsFromFirestore();
        model.addAttribute("posts", posts);

        // ✅ Lấy thông tin admin để hiển thị avatar & username
        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
            System.out.println("✅ Admin: " + adminUser.getUsername() + " - Avatar: " + adminUser.getAvatarUrl());
        } else {
            System.out.println("⚠ Không tìm thấy thông tin admin.");
        }

        System.out.println("✅ Admin vào dashboard thành công!");
        return "adminDashboard";
    }

    @PostMapping("/changeRole")
    public String changeUserRole(@RequestParam String username, @RequestParam User.Role newRole, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        userService.updateUserRole(username, newRole);
        return "redirect:/adminDashboard/dashboard";
    }

    @PostMapping("/deleteUser")
    public String deleteUser(@RequestParam String username, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        userService.deleteUser(username);
        return "redirect:/adminDashboard/dashboard";
    }

    @PostMapping("/deleteExam")
    public String deleteExam(@RequestParam String examId, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }
        examService.deleteExam(examId);
        return "redirect:/adminDashboard/dashboard";
    }

    @PostMapping("/deletePost")
    public String deletePost(@RequestParam String postId, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }
        PostService.deletePost(postId);
        return "redirect:/adminDashboard/dashboard";
    }
}
