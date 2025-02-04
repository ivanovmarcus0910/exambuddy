package com.example.exambuddy.controller;

import com.example.exambuddy.model.User;
import com.example.exambuddy.service.FirebaseAuthService;
import com.example.exambuddy.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.SessionAttribute;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

@Controller
public class AccountController {
    @Autowired
    private FirebaseAuthService authService;
//    @RequestMapping("/logout")
//    public String profilePage() {
//        return "profile";
//    }
    @RequestMapping("/profile")
    public String profilePage(HttpServletRequest request, Model model) {
        Cookie[] cookies = request.getCookies();
        String username = null;

        // Duyệt qua tất cả các cookie để tìm cookie có tên "rememberedUsername"
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("rememberedUsername".equals(cookie.getName())) {
                    try {
                        // Giải mã giá trị cookie nếu có
                        username = URLDecoder.decode(cookie.getValue(), "UTF-8");
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                    }
                    break;
                }
            }
        }

        if (username != null) {
            // Lấy thông tin người dùng từ dịch vụ với username đã tìm thấy
            UserService userService = new UserService();
            User user = userService.getUserData(username);
            model.addAttribute("user", user);
        } else {
            // Xử lý khi không có cookie "rememberedUsername"
            model.addAttribute("userinfo", null);
        }
        return "profile";
    }
    @PostMapping("/profile/upload")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file,
                               String username,
                               Model model) throws IOException {

        // Kiểm tra nếu file không rỗng
        if (!file.isEmpty()) {
            // Đặt tên file là username.jpg
            String fileName = username + ".jpg";
            // Lấy đường dẫn thư mục để lưu ảnh
            String uploadDir = "src/main/resources/static/img/";

            // Tạo file mới và lưu
            File dest = new File(uploadDir + fileName);
            file.transferTo(dest);

            // Lưu lại đường dẫn ảnh vào database hoặc thực hiện xử lý khác nếu cần

            // Thêm thông tin user vào model và trả về trang profile
            User user = UserService.getUserData(username);
            model.addAttribute("user", user);
        }

        return "profile";  // Trở về trang profile
    }
}
