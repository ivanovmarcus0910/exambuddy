package com.example.exambuddy.controller;

import com.example.exambuddy.model.User;
import com.example.exambuddy.service.FirebaseAuthService;
import com.example.exambuddy.service.PasswordService;
import com.example.exambuddy.service.UserService;
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
    private PasswordService passService;

    public AccountController(PasswordService passService) {
        this.passService = passService;
    }

    @RequestMapping("/profile")
    public String profilePage(HttpServletRequest request, Model model) {
        Cookie[] cookies = request.getCookies();
        String username = null;

        // Duyệt qua tất cả các cookie để tìm cookie có tên "rememberedUsername"
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("noname".equals(cookie.getName())) {
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
            model.addAttribute("user", null);
        }
        return "profile";
    }
    @PostMapping("/profile/upload")
    public String uploadAvatar(@RequestParam("avatar") MultipartFile file,
                               @RequestParam String username,
                               Model model) throws IOException {

        // Kiểm tra nếu file không rỗng
        if (!file.isEmpty()) {
            System.out.println("user name = "+username);
            System.out.println("user name = "+username);
            // Đặt tên file là username.jpg
            String fileName = username + ".jpg";
            // Lấy đường dẫn thư mục để lưu ảnh
            String uploadDir = "C:/upload/avatar/";

            // Tạo file mới và lưu
            File uploadFile = new File(uploadDir + fileName);
            if (uploadFile.exists()) {
                uploadFile.delete();  // Xóa file cũ
            }

            // Lưu file vào thư mục
            file.transferTo(uploadFile);

            // Lưu lại đường dẫn ảnh vào database hoặc thực hiện xử lý khác nếu cần

            // Thêm thông tin user vào model và trả về trang profile
            User user = UserService.getUserData(username);
            model.addAttribute("user", user);
        }

        return "profile";  // Trở về trang profile
    }
    @GetMapping("/avatar/{filename}")
    @ResponseBody
    public ResponseEntity<Resource> getAvatar(@PathVariable String filename) {
        Path filePath = Paths.get("C:/upload/avatar/").resolve(filename);
        Resource resource = new FileSystemResource(filePath);

        if (resource.exists()) {
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        }
        return ResponseEntity.notFound().build();
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
