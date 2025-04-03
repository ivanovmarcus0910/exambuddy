//package com.example.exambuddy.controller;
//
//import com.example.exambuddy.model.AdminLog;
//import com.example.exambuddy.model.User;
//import com.example.exambuddy.service.AdminLogService;
//import com.example.exambuddy.service.FirebaseAuthService;
//import com.example.exambuddy.service.UserService;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.data.domain.Page;
//import org.springframework.data.domain.PageRequest;
//import org.springframework.data.domain.Pageable;
//import org.springframework.data.domain.Sort;
//import org.springframework.stereotype.Controller;
//import org.springframework.ui.Model;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RequestParam;
//
//import jakarta.servlet.http.HttpSession;
//import java.util.ArrayList;
//import java.util.List;
//
//@Controller
//@RequestMapping("/admin/logs")
//public class AdminLogController {
//
//    @Autowired
//    private FirebaseAuthService authService;
//
//    @Autowired
//    private UserService userService;
//
//    @Autowired
//    private AdminLogService adminLogService;
//
//    @GetMapping("")
//    public String adminLogs(
//            @RequestParam(defaultValue = "0") int page,
//            @RequestParam(defaultValue = "10") int size,
//            @RequestParam(required = false) String searchQuery,
//            @RequestParam(required = false) String timeFilter,
//            @RequestParam(required = false, defaultValue = "formattedTimestamp") String sortBy,
//            @RequestParam(required = false, defaultValue = "desc") String sortDir,
//            Model model,
//            HttpSession session) throws Exception {
//
//        // Kiểm tra quyền admin
//        String loggedInUser = (String) session.getAttribute("loggedInUser");
//        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
//            return "redirect:/login";
//        }
//
//        // Lấy thông tin admin
//        User adminUser = userService.getUserByUsername(loggedInUser);
//        model.addAttribute("adminUser", adminUser);
//
//        // Tạo đối tượng Sort cho sắp xếp
//        Sort sort = Sort.by(sortDir.equalsIgnoreCase("asc") ? Sort.Direction.ASC : Sort.Direction.DESC, sortBy);
//
//        // Tạo đối tượng Pageable cho phân trang và sắp xếp
//        Pageable pageable = PageRequest.of(page, size, sort);
//
//        // Lấy danh sách log với phân trang, tìm kiếm và lọc
//        Page<AdminLog> logPage = adminLogService.getAdminLogs(pageable, searchQuery, timeFilter);
//
//        // Lấy danh sách log từ Page
//        List<AdminLog> logs = logPage.getContent();
//        if (logs == null) {
//            logs = new ArrayList<>(); // Đảm bảo logs không bao giờ là null
//        }
//
//        // Truyền dữ liệu vào model
//        model.addAttribute("logs", logs);
//        model.addAttribute("logsEmpty", logs.isEmpty()); // Thêm biến logsEmpty
//        model.addAttribute("currentPage", logPage.getNumber());
//        model.addAttribute("totalPages", logPage.getTotalPages());
//        model.addAttribute("totalItems", logPage.getTotalElements());
//        model.addAttribute("pageSize", size);
//        model.addAttribute("searchQuery", searchQuery);
//        model.addAttribute("timeFilter", timeFilter);
//        model.addAttribute("sortBy", sortBy);
//        model.addAttribute("sortDir", sortDir);
//
//        return "adminLogs"; // File Thymeleaf hiển thị log của admin
//    }
//}