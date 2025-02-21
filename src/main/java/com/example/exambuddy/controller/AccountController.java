package com.example.exambuddy.controller;

import com.example.exambuddy.model.User;
import com.example.exambuddy.service.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.Path;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.core.io.Resource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import java.nio.file.Path;
import java.nio.file.Paths;
@Controller
public class AccountController {
    @Autowired
    private FirebaseAuthService authService;
    @Autowired
    private PasswordService passService;
    @Autowired

    private CloudinaryService cloudinaryService=new CloudinaryService();
    @Autowired
    private CookieService cookieService;
    public AccountController(PasswordService passService) {
        this.passService = passService;
    }

    @RequestMapping("/profile")
    public String profilePage(HttpServletRequest request, Model model) {

        // Duyệt qua tất cả các cookie để tìm cookie có tên "noname"

        String username = URLDecoder.decode(cookieService.getCookie(request,"noname"));

        if (username != null) {
            // Lấy thông tin người dùng từ dịch vụ với username đã tìm thấy
            UserService userService = new UserService();
            User user = userService.getUserData(username);
            model.addAttribute("user", user);
        } else {
            model.addAttribute("user", null);
        }
        return "profile";
    }
    @PostMapping("/profile/upload")
    public String uploadAvatar(@RequestParam("image") MultipartFile file,
                               @RequestParam String username,
                               Model model) throws IOException {
        String url = this.cloudinaryService.upLoadImg(file, "imgAvatar");
        System.out.println("URL="+url);
        UserService.changeAvatar(username, url);
            User user = UserService.getUserData(username);
        model.addAttribute("user", user);

        return "profile";  // Trở về trang profile
    }

    /**
    Thay đổi mật khẩu
     */
    @GetMapping("/changePass")
    public String changePasswordPage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về login
        }
        return "changePass"; // Trả về trang HTML để đổi mật khẩu
    }

    @PostMapping("/changePass")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 Model model) {

        // 🔥 Lấy username của người dùng đã đăng nhập từ session
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về login
        }

        // 🔍 Kiểm tra xác nhận mật khẩu mới
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "changePass";
        }

        System.out.println("📌 Đang thực hiện đổi mật khẩu cho username: " + username);

        // ✅ Gọi `updatePasswordForLoggedInUser` để kiểm tra mật khẩu hiện tại & cập nhật mật khẩu mới
        boolean isUpdated = passService.updatePasswordForUser(username, currentPassword, newPassword);
        if (isUpdated) {
            model.addAttribute("success", "Mật khẩu đã cập nhật thành công!");
        } else {
            model.addAttribute("error", "Mật khẩu hiện tại không đúng hoặc có lỗi khi cập nhật!");
        }

        return "changePass";
    }

}
