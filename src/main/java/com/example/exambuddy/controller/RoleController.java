package com.example.exambuddy.controller;

import com.example.exambuddy.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

@Controller
public class RoleController {
    @Autowired
    private UserService userService;

    // Phương thức chuyển đổi Role sang tên tiếng Việt
    public String getRoleNameInVietnamese(String role) {
        switch (role) {
            case "ADMIN":
                return "Quản trị viên";
            case "STUDENT":
                return "Học viên";
            case "TEACHER":
                return "Giảng viên";
            case "PENDING_TEACHER":
                return "Giảng viên chờ duyệt";
            case "UPGRADED_STUDENT":
                return "Học viên nâng cấp";
            default:
                return "Chưa xác định";
        }
    }
}
