package com.example.exambuddy.controller;

import com.example.exambuddy.model.AdminLog;
import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.Post;
import com.example.exambuddy.model.User;
import com.example.exambuddy.service.*;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.concurrent.CompletableFuture;
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

    @Autowired
    private EmailService emailService;

    @Autowired
    private PasswordService passwordService;

    @Autowired
    private AdminLogService adminLogService;

    // Phương thức chuyển đổi vai trò sang tiếng Việt
    private String convertRoleToVietnamese(String role) {
        switch (role.toUpperCase()) {
            case "TEACHER":
                return "Giáo viên";
            case "STUDENT":
                return "Học sinh";
            case "UPGRADED_STUDENT":
                return "Học sinh nâng cấp";
            case "PENDING_TEACHER":
                return "Giáo viên chờ duyệt";
            case "ADMIN":
                return "Quản trị viên";
            default:
                return role;
        }
    }

    // Phương thức chuyển đổi trạng thái sang tiếng Việt
    private String convertStatusToVietnamese(String status) {
        switch (status.toUpperCase()) {
            case "ACTIVE":
                return "Kích hoạt";
            case "INACTIVE":
                return "Vô hiệu hóa";
            case "APPROVED":
                return "Đã phê duyệt";
            case "REJECTED":
                return "Bị từ chối";
            case "DISABLED":
                return "Vô hiệu hóa";
            default:
                return status;
        }
    }

    // Trang chủ (Dashboard): admin.html
    @GetMapping("")
    public CompletableFuture<String> adminDashboard(Model model, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return CompletableFuture.completedFuture("redirect:/login");
        }

        long startTime = System.currentTimeMillis(); // Đo thời gian

        // Lấy dữ liệu bất đồng bộ
        CompletableFuture<List<User>> usersFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return userService.getUserPage(0, 100); // Chỉ lấy 100 người dùng đầu tiên
            } catch (Exception e) {
                e.printStackTrace();
                return List.of();
            }
        });

        CompletableFuture<List<Exam>> examsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return examService.getAllExams(); // Có thể thay bằng phân trang nếu cần
            } catch (Exception e) {
                e.printStackTrace();
                return List.of();
            }
        });

        CompletableFuture<List<Post>> postsFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return postService.getPublicPostsFromFirestore(); // Có thể thay bằng phân trang nếu cần
            } catch (Exception e) {
                e.printStackTrace();
                return List.of();
            }
        });

        CompletableFuture<Page<AdminLog>> logsFuture = CompletableFuture.supplyAsync(() -> {
            PageRequest pageable = PageRequest.of(0, 3, Sort.by(Sort.Direction.DESC, "timestamp"));
            try {
                return adminLogService.getAdminLogs(pageable, null, null);
            } catch (ExecutionException e) {
                throw new RuntimeException(e);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        });

        return CompletableFuture.allOf(usersFuture, examsFuture, postsFuture, logsFuture)
                .thenApply(v -> {
                    try {
                        List<User> users = usersFuture.get();
                        List<Exam> exams = examsFuture.get();
                        List<Post> posts = postsFuture.get();
                        Page<AdminLog> recentLogs = logsFuture.get();

                        // Thêm dữ liệu vào model
                        model.addAttribute("totalUser", users.size());
                        model.addAttribute("totalExam", exams.size());
                        model.addAttribute("totalPost", posts.size());

                        // Đếm vai trò bất đồng bộ hoặc dùng truy vấn Firestore trực tiếp
                        long adminCount = users.stream()
                                .filter(u -> u.getRole() != null && u.getRole().toString().toLowerCase().contains("admin"))
                                .count();
                        long teacherCount = users.stream()
                                .filter(u -> u.getRole() != null && u.getRole().toString().toLowerCase().contains("teacher"))
                                .count();
                        long studentCount = users.stream()
                                .filter(u -> u.getRole() != null && (u.getRole().toString().toLowerCase().contains("student") || u.getRole().toString().toLowerCase().contains("upgraded_student")))
                                .count();
                        long pendingCount = users.stream()
                                .filter(u -> u.getRole() != null && u.getRole().toString().toLowerCase().contains("pending"))
                                .count();
                        long blockedCount = users.stream()
                                .filter(u -> u.getRole() != null && u.getRole().toString().toLowerCase().contains("blocked"))
                                .count();

                        model.addAttribute("adminCount", adminCount);
                        model.addAttribute("teacherCount", teacherCount);
                        model.addAttribute("studentCount", studentCount);
                        model.addAttribute("pendingCount", pendingCount);
                        model.addAttribute("blockedCount", blockedCount);

                        // Lấy 3 hành động gần đây nhất
                        model.addAttribute("recentLogs", recentLogs.getContent());
                        model.addAttribute("recentLogsEmpty", recentLogs.isEmpty());

                        // Số lượng báo cáo chưa xử lý (giả sử)
                        long pendingReportsCount = 0; // Thay bằng logic thực tế
                        model.addAttribute("pendingReportsCount", pendingReportsCount);

                        // Thông tin admin
                        User adminUser = userService.getUserByUsername(loggedInUser);
                        if (adminUser != null) {
                            model.addAttribute("adminUser", adminUser);
                        }

                        long endTime = System.currentTimeMillis();
                        System.out.println("Request to /admin took " + (endTime - startTime) + " ms");

                        return "adminDashboard";
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "redirect:/login";
                    }
                });
    }

    // Quản lý Người dùng: adminUser.html
    @GetMapping("/users")
    public String adminUsers(Model model, HttpSession session,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String search) throws Exception {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        int pageSize = 10;
        List<User> userPage;
        if (search != null && !search.trim().isEmpty()) {
            userPage = userService.searchUsers(search.trim(), page, pageSize);
            model.addAttribute("searchParam", search.trim());
        } else {
            userPage = userService.getUserPage(page, pageSize);
        }
        model.addAttribute("userPage", userPage);
        model.addAttribute("currentPage", page);

        List<User> allUsers;
        if (search != null && !search.trim().isEmpty()) {
            allUsers = userService.searchUsers(search.trim());
        } else {
            allUsers = userService.getAllUsers();
        }

        boolean hasNextPage = userPage.size() == pageSize;
        model.addAttribute("hasNextPage", hasNextPage);

        if (userPage.size() < pageSize && page > 0) {
            model.addAttribute("emptyPageMessage", "Trang tiếp theo không có dữ liệu.");
        }

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
        List<User> inactiveUsers = allUsers.stream()
                .filter(u -> !u.isActive())
                .collect(Collectors.toList());
        List<User> unverifiedUsers = allUsers.stream()
                .filter(u -> !u.isVerified())
                .collect(Collectors.toList());

        model.addAttribute("allUsers", allUsers);
        model.addAttribute("students", students);
        model.addAttribute("teachers", teachers);
        model.addAttribute("upgraded", upgraded);
        model.addAttribute("pending", pending);
        model.addAttribute("admins", admins);
        model.addAttribute("inactiveUsers", inactiveUsers);
        model.addAttribute("unverifiedUsers", unverifiedUsers);

        model.addAttribute("studentCount", students.size());
        model.addAttribute("teacherCount", teachers.size());
        model.addAttribute("upgradedCount", upgraded.size());
        model.addAttribute("pendingCount", pending.size());
        model.addAttribute("adminCount", admins.size());

        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
        }

        return "adminUser";
    }

    @PostMapping("/addUser")
    public String addUser(@RequestParam String username,
                          @RequestParam String email,
                          @RequestParam String password,
                          @RequestParam String role,
                          RedirectAttributes redirectAttributes,
                          Model model,
                          HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        CompletableFuture<Boolean> usernameExistsFuture = authService.isUsernameExists(username);
        CompletableFuture<Boolean> emailExistsFuture = authService.isEmailExists(email);

        return CompletableFuture.allOf(usernameExistsFuture, emailExistsFuture)
                .thenApply(v -> {
                    boolean hasError = false;
                    if (!passwordService.isValidPassword(password)) {
                        model.addAttribute("passwordError", "Mật khẩu phải có ít nhất 8 ký tự, bao gồm chữ hoa, chữ thường, số và ký tự đặc biệt!");
                        hasError = true;
                    }

                    try {
                        if (usernameExistsFuture.get()) {
                            model.addAttribute("usernameError", "Tên đăng nhập đã tồn tại!");
                            hasError = true;
                        }
                        if (emailExistsFuture.get()) {
                            model.addAttribute("emailError", "Email đã được sử dụng!");
                            hasError = true;
                        }
                    } catch (Exception e) {
                        model.addAttribute("error", "Lỗi kiểm tra thông tin: " + e.getMessage());
                        hasError = true;
                    }

                    model.addAttribute("usernameValue", username);
                    model.addAttribute("emailValue", email);
                    model.addAttribute("roleValue", role);

                    User adminUser = userService.getUserByUsername(loggedInUser);
                    if (adminUser != null) {
                        model.addAttribute("adminUser", adminUser);
                    }

                    if (hasError) {
                        int page = 0;
                        int pageSize = 10;

                        try {
                            List<User> userPage = userService.getUserPage(page, pageSize);
                            model.addAttribute("userPage", userPage);
                            model.addAttribute("currentPage", page);
                            model.addAttribute("hasNextPage", userPage.size() == pageSize);
                            model.addAttribute("showNewUserModal", true);
                        } catch (Exception e) {
                            model.addAttribute("error", "Lỗi tải danh sách người dùng: " + e.getMessage());
                        }
                        return "adminUser";
                    }

                    User.Role userRole;
                    try {
                        userRole = User.Role.valueOf(role.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        userRole = User.Role.STUDENT;
                    }

                    String result = authService.registerUser(email, username, password, userRole);
                    AdminLog log = new AdminLog(
                            loggedInUser,
                            "ADD_USER",
                            "User",
                            username,
                            username,
                            "Admin đã thêm người dùng mới với tên đăng nhập: " + username + ", vai trò: " + convertRoleToVietnamese(userRole.toString())
                    );

                    try {
                        adminLogService.logAction(log);
                    } catch (Exception e) {
                        model.addAttribute("error", "Lỗi ghi log hành động: " + e.getMessage());
                        return "adminUser";
                    }

                    redirectAttributes.addFlashAttribute("successMessage", "🎉 Đã thêm người dùng '" + username + "' thành công!");
                    return "redirect:/admin/users";
                })
                .exceptionally(throwable -> {
                    model.addAttribute("error", "Lỗi hệ thống: " + throwable.getMessage());
                    model.addAttribute("usernameValue", username);
                    model.addAttribute("emailValue", email);
                    model.addAttribute("roleValue", role);
                    return "adminUser";
                })
                .join();
    }

    @PostMapping("/approveTeacher")
    public String approveTeacher(@RequestParam String username, HttpSession session, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(username);
        if (user != null) {
            userService.updateUserRole(username, User.Role.TEACHER);
            emailService.sendTeacherStatusNotification(user.getEmail(), true, user);

            AdminLog log = new AdminLog(
                    loggedInUser,
                    "APPROVE_TEACHER",
                    "User",
                    username,
                    username,
                    "Admin đã phê duyệt yêu cầu làm giáo viên của người dùng: " + username
            );
            try {
                adminLogService.logAction(log);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi ghi log hành động: " + e.getMessage());
            }
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/rejectTeacher")
    public String rejectTeacher(@RequestParam String username, HttpSession session, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(username);
        if (user != null) {
            userService.updateUserRole(username, User.Role.PENDING_TEACHER);
            emailService.sendTeacherStatusNotification(user.getEmail(), false, user);

            AdminLog log = new AdminLog(
                    loggedInUser,
                    "REJECT_TEACHER",
                    "User",
                    username,
                    username,
                    "Admin đã từ chối yêu cầu làm giáo viên của người dùng: " + username
            );
            try {
                adminLogService.logAction(log);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi ghi log hành động: " + e.getMessage());
            }
        }

        return "redirect:/admin/users";
    }

    @GetMapping("/exams")
    public String adminExams(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        List<Exam> exams = examService.getAllExams();
        model.addAttribute("exams", exams);
        model.addAttribute("isAdmin", true);
        model.addAttribute("currentUser", userService.getUserByUsername(loggedInUser));

        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
        }

        return "adminExam";
    }

    @PostMapping("/exams/delete/{examId}")
    public String deleteExam(@PathVariable String examId, HttpSession session, Model model, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            session.setAttribute("error", "Bạn cần đăng nhập.");
            return "redirect:/login";
        }

        Exam exam = examService.getExam(examId);
        if (exam != null && exam.getUsername().equals(username)) {
            examService.disableExam(examId);

            AdminLog log = new AdminLog(
                    username,
                    "DISABLE_EXAM",
                    "Exam",
                    examId,
                    exam.getExamName(),
                    "Admin đã vô hiệu hóa đề thi với ID: " + examId
            );
            try {
                adminLogService.logAction(log);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi ghi log hành động: " + e.getMessage());
            }

            redirectAttributes.addFlashAttribute("successMessage", "Đề thi đã được vô hiệu hoá thành công!");
            return "redirect:/exams/created";
        } else {
            model.addAttribute("error", "Bạn không có quyền xoá đề thi này hoặc đề thi không tồn tại!");
            return "error";
        }
    }

    @GetMapping("/posts")
    public String adminPosts(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        List<Post> posts = postService.getPostsFromFirestore();
        model.addAttribute("posts", posts);

        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
        }

        return "adminPost";
    }

    @GetMapping("/stats")
    public String adminStat(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        List<User> users = userService.getAllUsers();
        List<Exam> exams = examService.getAllExams();
        List<Post> posts = postService.getPublicPostsFromFirestore();

        model.addAttribute("totalUser", users.size());
        model.addAttribute("totalExam", exams.size());
        model.addAttribute("totalPost", posts.size());

        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
        }

        return "adminStat";
    }

    @GetMapping("/logs")
    public CompletableFuture<String> adminLogs(Model model, HttpSession session,
                                               @RequestParam(defaultValue = "0") int page,
                                               @RequestParam(defaultValue = "10") int size,
                                               @RequestParam(required = false) String searchQuery,
                                               @RequestParam(required = false) String timeFilter,
                                               @RequestParam(defaultValue = "timestamp") String sortBy,
                                               @RequestParam(defaultValue = "desc") String sortDir) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return CompletableFuture.completedFuture("redirect:/login");
        }

        long startTime = System.currentTimeMillis(); // Đo thời gian

        Sort.Direction direction = sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC;
        PageRequest pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        // Lấy log bất đồng bộ
        CompletableFuture<Page<AdminLog>> logFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return adminLogService.getAdminLogs(pageable, searchQuery, timeFilter);
            } catch (Exception e) {
                e.printStackTrace();
                return Page.empty();
            }
        });

        // Lấy thông tin admin bất đồng bộ
        CompletableFuture<User> adminUserFuture = CompletableFuture.supplyAsync(() -> {
            try {
                return userService.getUserByUsername(loggedInUser);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        });

        return CompletableFuture.allOf(logFuture, adminUserFuture)
                .thenApply(v -> {
                    try {
                        Page<AdminLog> logPage = logFuture.get();
                        User adminUser = adminUserFuture.get();

                        model.addAttribute("logs", logPage.getContent());
                        model.addAttribute("logsEmpty", logPage.isEmpty());
                        model.addAttribute("currentPage", logPage.getNumber());
                        model.addAttribute("totalPages", logPage.getTotalPages());
                        model.addAttribute("pageSize", logPage.getSize());
                        model.addAttribute("totalItems", logPage.getTotalElements());
                        model.addAttribute("searchQuery", searchQuery);
                        model.addAttribute("timeFilter", timeFilter);
                        model.addAttribute("sortBy", sortBy);
                        model.addAttribute("sortDir", sortDir);

                        if (adminUser != null) {
                            model.addAttribute("adminUser", adminUser);
                        }

                        long endTime = System.currentTimeMillis();
                        System.out.println("Request to /admin/logs took " + (endTime - startTime) + " ms");

                        return "adminLogs";
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "redirect:/login";
                    }
                });
    }

    @PostMapping("/changeRole")
    public String changeUserRole(@RequestParam String username, @RequestParam User.Role newRole, HttpSession session, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        userService.updateUserRole(username, newRole);

        AdminLog log = new AdminLog(
                loggedInUser,
                "CHANGE_USER_ROLE",
                "User",
                username,
                username,
                "Admin đã thay đổi vai trò của người dùng " + username + " thành " + convertRoleToVietnamese(newRole.toString())
        );
        try {
            adminLogService.logAction(log);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Lỗi ghi log hành động: " + e.getMessage());
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/updateUserStatus")
    public String updateUserStatus(@RequestParam String username, HttpSession session, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(username);
        if (user != null) {
            boolean newStatus = !user.isActive();
            user.setActive(newStatus);
            userService.updateUserStatus(username, newStatus);

            AdminLog log = new AdminLog(
                    loggedInUser,
                    "UPDATE_USER_STATUS",
                    "User",
                    username,
                    username,
                    "Admin đã cập nhật trạng thái của người dùng " + username + " thành " + (newStatus ? "Kích hoạt" : "Vô hiệu hóa")
            );
            try {
                adminLogService.logAction(log);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi ghi log hành động: " + e.getMessage());
            }
        }

        return "redirect:/admin/users";
    }

    @PostMapping("/updateExamStatus")
    public String updateExamStatus(@RequestParam String examId, @RequestParam String status,
                                   HttpSession session, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        Exam exam = examService.getExam(examId);
        if (exam != null) {
            exam.setStatus(status);
            examService.updateExamStatus(examId, status);

            AdminLog log = new AdminLog(
                    loggedInUser,
                    "UPDATE_EXAM_STATUS",
                    "Exam",
                    examId,
                    exam.getExamName(),
                    "Admin đã cập nhật trạng thái của đề thi " + examId + " thành " + convertStatusToVietnamese(status)
            );
            try {
                adminLogService.logAction(log);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi ghi log hành động: " + e.getMessage());
            }

            redirectAttributes.addFlashAttribute("successMessage", "Cập nhật trạng thái đề thi thành công!");
        }

        return "redirect:/admin/exams";
    }

    @PostMapping("/updatePostStatus")
    public String updatePostStatus(@RequestParam String postId,
                                   @RequestParam(required = false, defaultValue = "/admin/posts") String redirectTo,
                                   HttpSession session, RedirectAttributes redirectAttributes) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        Post post = postService.getPostById(postId);
        if (post != null) {
            boolean newStatus = !post.isActive();
            post.setActive(newStatus);
            postService.updatePostStatus(postId, newStatus);

            AdminLog log = new AdminLog(
                    loggedInUser,
                    "UPDATE_POST_STATUS",
                    "Post",
                    postId,
                    post.getContent(),
                    "Admin đã cập nhật trạng thái của bài viết " + postId + " thành " + (newStatus ? "Kích hoạt" : "Vô hiệu hóa")
            );
            try {
                adminLogService.logAction(log);
            } catch (Exception e) {
                redirectAttributes.addFlashAttribute("errorMessage", "Lỗi ghi log hành động: " + e.getMessage());
            }
        }

        return "redirect:" + redirectTo;
    }
}