package com.example.exambuddy.controller;

import com.example.exambuddy.model.User;
import com.example.exambuddy.service.FirebaseAuthService;
import com.example.exambuddy.service.UserService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/adminDashboard")
public class AdminController {
    @Autowired
    private FirebaseAuthService authService;

    @Autowired
    private UserService userService;

    // ✅ Chỉ Admin mới có thể truy cập trang này
    @GetMapping("/dashboard")
    public String adminDashboard(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        System.out.println("📌 Session hiện tại: " + loggedInUser);

        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            System.out.println("❌ Không có user trong session hoặc không phải admin. Chuyển về login.");
            return "redirect:/login";
        }

        List<User> users = userService.getAllUsers();
        model.addAttribute("users", users);

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
}
