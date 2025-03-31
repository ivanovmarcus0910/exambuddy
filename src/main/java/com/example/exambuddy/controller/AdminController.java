package com.example.exambuddy.controller;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.Post;
import com.example.exambuddy.model.User;
import com.example.exambuddy.service.*;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
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
        List<Post> posts = PostService.getPublicPostsFromFirestore();

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
    public String adminUsers(Model model, HttpSession session,
                             @RequestParam(defaultValue = "0") int page,
                             @RequestParam(required = false) String search) throws Exception {

        // Kiểm tra admin đã đăng nhập hay chưa
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        int pageSize = 10;
        List<User> userPage;
        // Nếu có từ khóa tìm kiếm, lọc theo username, email, và tên
        if (search != null && !search.trim().isEmpty()) {
            userPage = userService.searchUsers(search.trim(), page, pageSize);
            model.addAttribute("searchParam", search.trim());
        } else {
            userPage = userService.getUserPage(page, pageSize);
        }
        model.addAttribute("userPage", userPage);
        model.addAttribute("currentPage", page);

        // Lấy toàn bộ người dùng
        List<User> allUsers;
        if (search != null && !search.trim().isEmpty()) {
            allUsers = userService.searchUsers(search.trim());
        } else {
            allUsers = userService.getAllUsers();
        }

        // Kiểm tra nếu trang hiện tại chưa đủ 10 phần tử
        boolean hasNextPage = userPage.size() == pageSize;
        model.addAttribute("hasNextPage", hasNextPage);

        // Nếu không có dữ liệu ở trang tiếp theo, hiển thị thông báo
        if (userPage.size() < pageSize && page > 0) {
            model.addAttribute("emptyPageMessage", "Trang tiếp theo không có dữ liệu.");
        }


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
        List<User> inactiveUsers = allUsers.stream()
                .filter(u -> !u.isActive())
                .collect(Collectors.toList());
        List<User> unverifiedUsers = allUsers.stream()
                .filter(u -> !u.isVerified())
                .collect(Collectors.toList());

        model.addAttribute("allUsers", allUsers);      // Cho tab "Tổng số"
        model.addAttribute("students", students);      // Cho tab "Học sinh"
        model.addAttribute("teachers", teachers);      // Cho tab "Giáo viên"
        model.addAttribute("upgraded", upgraded);
        model.addAttribute("pending", pending);// Cho tab "Upgrade học sinh"
        model.addAttribute("admins", admins);
        model.addAttribute("inactiveUsers", inactiveUsers);
        model.addAttribute("unverifiedUsers", unverifiedUsers);// Cho tab "Admin"

        // Thêm số liệu thống kê cho biểu đồ
        model.addAttribute("studentCount", students.size());
        model.addAttribute("teacherCount", teachers.size());
        model.addAttribute("upgradedCount", upgraded.size());
        model.addAttribute("pendingCount", pending.size());
        model.addAttribute("adminCount", admins.size());
        // Thông tin admin
        User adminUser = userService.getUserByUsername(loggedInUser);
        if (adminUser != null) {
            model.addAttribute("adminUser", adminUser);
        }

        return "adminUser"; // Trả về view adminUser.html (đã gộp chức năng tab)
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

        // Gọi các phương thức bất đồng bộ
        CompletableFuture<Boolean> usernameExistsFuture = authService.isUsernameExists(username);
        CompletableFuture<Boolean> emailExistsFuture = authService.isEmailExists(email);

        // Xử lý bất đồng bộ toàn bộ logic
        return CompletableFuture.allOf(usernameExistsFuture, emailExistsFuture)
                .thenApply(v -> {
                    boolean hasError = false;
                    // Kiểm tra mật khẩu với điều kiện mới
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
                        e.printStackTrace();
                        hasError = true;
                    }

                    // Giữ lại giá trị đã nhập để hiển thị trong form
                    model.addAttribute("usernameValue", username);
                    model.addAttribute("emailValue", email);
                    model.addAttribute("roleValue", role);

                    // Lấy thông tin admin
                    User adminUser = userService.getUserByUsername(loggedInUser);
                    if (adminUser != null) {
                        model.addAttribute("adminUser", adminUser);
                    }

                    // Nếu có lỗi, cung cấp dữ liệu tối ưu cho template adminUser.html
                    if (hasError) {
                        int page = 0;
                        int pageSize = 10;

                        try {
                            // Chỉ tải danh sách người dùng cho trang hiện tại
                            List<User> userPage = userService.getUserPage(page, pageSize);
                            model.addAttribute("userPage", userPage);
                            model.addAttribute("currentPage", page);
                            model.addAttribute("hasNextPage", userPage.size() == pageSize);

                            // Không tải toàn bộ danh sách người dùng ngay lập tức
                            // Các tab (students, teachers, v.v.) sẽ được tải qua AJAX nếu cần
                            model.addAttribute("showNewUserModal", true);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        return "adminUser";
                    }

                    // Nếu không có lỗi, tiến hành đăng ký người dùng
                    User.Role userRole;
                    try {
                        userRole = User.Role.valueOf(role.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        userRole = User.Role.STUDENT;
                    }

                    String result = authService.registerUser(email, username, password, userRole);
                    redirectAttributes.addFlashAttribute("successMessage", "🎉 Đã thêm người dùng '" + username + "' thành công!");
                    return "redirect:/admin/users";
                })
                .exceptionally(throwable -> {
                    // Xử lý lỗi hệ thống
                    model.addAttribute("error", "Lỗi hệ thống: " + throwable.getMessage());
                    model.addAttribute("usernameValue", username);
                    model.addAttribute("emailValue", email);
                    model.addAttribute("roleValue", role);
                    return "adminUser";
                })
                .join(); // Đợi kết quả và trả về
    }

    // POST: Xác nhận yêu cầu trở thành giáo viên
    @PostMapping("/approveTeacher")
    public String approveTeacher(@RequestParam String username, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        User user = userService.getUserByUsername(username);
        if (user != null) {
            userService.updateUserRole(username, User.Role.TEACHER);

            // Gửi thông báo về việc yêu cầu đã được chấp nhận
            emailService.sendTeacherStatusNotification(user.getEmail(), true,user);  // Gửi email thông báo đã chấp nhận

            // Gửi thông báo tới người dùng về việc yêu cầu đã được chấp nhận
            //userService.sendNotification(user, "Yêu cầu của bạn đã được chấp nhận. Bạn đã trở thành giáo viên.");
        }

        return "redirect:/admin/users";  // Quay lại danh sách người dùng
    }

    // POST: Từ chối yêu cầu trở thành giáo viên
    @PostMapping("/rejectTeacher")
    public String rejectTeacher(@RequestParam String username, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }

        // Gửi thông báo cho người dùng
        User user = userService.getUserByUsername(username);
        if (user != null) {
            userService.updateUserRole(username, User.Role.PENDING_TEACHER);

            // Gửi thông báo về việc yêu cầu đã bị từ chối
            emailService.sendTeacherStatusNotification(user.getEmail(), false, user);  // Gửi email thông báo đã từ chối

            // Gửi thông báo tới người dùng về việc yêu cầu đã bị từ chối
            //userService.sendNotification(user, "Yêu cầu của bạn đã bị từ chối.");
        }

        return "redirect:/admin/users";  // Quay lại danh sách người dùng
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
        List<Post> posts = PostService.getPublicPostsFromFirestore();

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
    public String updatePostStatus(@RequestParam String postId,
                                   @RequestParam(required = false, defaultValue = "/admin/posts") String redirectTo,
                                   HttpSession session) {
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

        return "redirect:" + redirectTo;
    }

}