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
                return "Học sinh";
            case "TEACHER":
                return "Giáo viên";
            case "PENDING_TEACHER":
                return "Xét duyệt giáo viên";
            case "UPGRADED_STUDENT":
                return "Tài khoản nâng cấp";
            default:
                return "Chưa xác định";
        }
    }
}
