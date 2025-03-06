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

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
public class AdminController {

    @Autowired
    private FirebaseAuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private ExamService examService;

    @Autowired
    private PostService postService;

    // Trang chủ (Dashboard): admin.html
    @GetMapping("")
    public String adminDashboard(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        // Lấy số liệu tổng hợp (dùng để hiển thị trên trang dashboard)
        List<User> users = userService.getAllUsers();
        List<Exam> exams = examService.getAllExams();
        List<Post> posts = PostService.getPostsFromFirestore();

        model.addAttribute("totalUser", users.size());
        model.addAttribute("totalExam", exams.size());
        model.addAttribute("totalPost", posts.size());

        // Lấy thông tin admin
        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
        }

        return "adminDashboard"; // Trả về admin.html
    }

    // Quản lý Người dùng: adminUser.html
    // Quản lý Người dùng (gộp danh sách và phân loại theo tab vào 1 trang adminUser.html)
    @GetMapping("/users")
    public String adminUsers(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        // Lấy toàn bộ người dùng
        List<User> allUsers = userService.getAllUsers();

        // Phân loại theo vai trò
        List<User> students = allUsers.stream()
                .filter(u -> "STUDENT".equalsIgnoreCase(u.getRole().toString()))
                .collect(Collectors.toList());
        List<User> teachers = allUsers.stream()
                .filter(u -> "TEACHER".equalsIgnoreCase(u.getRole().toString()))
                .collect(Collectors.toList());
        List<User> upgraded = allUsers.stream()
                .filter(u -> "UPGRADED_STUDENT".equalsIgnoreCase(u.getRole().toString()))
                .collect(Collectors.toList());
        List<User> pending = allUsers.stream()
                .filter(u -> "PENDING_TEACHER".equalsIgnoreCase(u.getRole().toString()))
                .collect(Collectors.toList());
        List<User> admins = allUsers.stream()
                .filter(u -> "ADMIN".equalsIgnoreCase(u.getRole().toString()))
                .collect(Collectors.toList());

        // Đưa dữ liệu vào model
        model.addAttribute("users", allUsers);       // Dữ liệu tổng nếu cần
        model.addAttribute("allUsers", allUsers);      // Cho tab "Tổng số"
        model.addAttribute("students", students);      // Cho tab "Học sinh"
        model.addAttribute("teachers", teachers);      // Cho tab "Giáo viên"
        model.addAttribute("upgraded", upgraded);
        model.addAttribute("pending", pending);// Cho tab "Upgrade học sinh"
        model.addAttribute("admins", admins);          // Cho tab "Admin"

        // Thông tin admin
        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
        }

        return "adminUser"; // Trả về view adminUser.html (đã gộp chức năng tab)
    }

    // Quản lý Đề thi: adminExam.html
    @GetMapping("/exams")
    public String adminExams(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        List<Exam> exams = examService.getAllExams();
        model.addAttribute("exams", exams);

        // Thông tin admin
        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
        }

        return "adminExam"; // Trả về adminExam.html
    }

    // Quản lý Bài đăng: adminPost.html
    @GetMapping("/posts")
    public String adminPosts(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        List<Post> posts = PostService.getPostsFromFirestore();
        model.addAttribute("posts", posts);

        // Thông tin admin
        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
        }

        return "adminPost"; // Trả về adminPost.html
    }

    // Thống kê & Biểu đồ: adminStat.html
    @GetMapping("/stats")
    public String adminStat(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        // Lấy số liệu tổng hợp
        List<User> users = userService.getAllUsers();
        List<Exam> exams = examService.getAllExams();
        List<Post> posts = PostService.getPostsFromFirestore();

        model.addAttribute("totalUser", users.size());
        model.addAttribute("totalExam", exams.size());
        model.addAttribute("totalPost", posts.size());

        // Thông tin admin
        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
        }

        return "adminStat"; // Trả về adminStat.html
    }

    // POST: Cập nhật vai trò người dùng
    @PostMapping("/changeRole")
    public String changeUserRole(@RequestParam String username, @RequestParam User.Role newRole, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        userService.updateUserRole(username, newRole);
        return "redirect:/admin/users";
    }

    // POST: Cập nhật trạng thái hoạt động của người dùng
    @PostMapping("/updateUserStatus")
    public String updateUserStatus(@RequestParam String username, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(username);
        if (user != null) {
            boolean newStatus = !user.isActive();
            user.setActive(newStatus);
            userService.updateUserStatus(username, newStatus);
        }
        return "redirect:/admin/users";
    }

    // POST: Cập nhật trạng thái hoạt động của đề thi
    @PostMapping("/updateExamStatus")
    public String updateExamStatus(@RequestParam String examId, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        Exam exam = examService.getExam(examId);
        if (exam != null) {
            boolean newStatus = !exam.isActive();
            exam.setActive(newStatus);
            examService.updateExamStatus(examId, newStatus);
        }
        return "redirect:/admin/exams";
    }

    // POST: Cập nhật trạng thái hoạt động của bài đăng
    @PostMapping("/updatePostStatus")
    public String updatePostStatus(@RequestParam String postId, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        Post post = postService.getPostById(postId);
        if (post != null) {
            boolean newStatus = !post.isActive();
            post.setActive(newStatus);
            postService.updatePostStatus(postId, newStatus);
        }
        return "redirect:/admin/posts";
    }
}
