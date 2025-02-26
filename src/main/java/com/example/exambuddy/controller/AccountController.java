package com.example.exambuddy.controller;

import com.example.exambuddy.model.Payment;
import com.example.exambuddy.model.User;
import com.example.exambuddy.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.util.List;
import java.util.concurrent.ExecutionException;

import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AccountController {
    @Autowired
    private FirebaseAuthService authService;
    @Autowired
    private PasswordService passService;
    @Autowired
    private CloudinaryService cloudinaryService = new CloudinaryService();
    @Autowired
    private CookieService cookieService;
    @Autowired
    private UserService userService;

    public AccountController(PasswordService passService) {
        this.passService = passService;
    }

    @RequestMapping("/profile")
    public String profilePage(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "redirect:/login";
        }

        // Lấy thông tin người dùng từ dịch vụ với username đã tìm thấy
        UserService userService = new UserService();
        User user = userService.getUserData(username);
        model.addAttribute("user", user);

        return "profile";
    }

    @PostMapping("/profile/upload")
    public String uploadAvatar(@RequestParam("image") MultipartFile file,
                               @RequestParam String username,
                               HttpSession session,
                               Model model) throws IOException {
        String url = this.cloudinaryService.upLoadImg(file, "imgAvatar");
        System.out.println("URL=" + url);
        UserService.changeAvatar(username, url);
        session.setAttribute("urlimg", url);
        User user = UserService.getUserData(username);
        model.addAttribute("user", user);

        return "redirect:/profile";  // Trở về trang profile
    }

    @PostMapping("/profile/update")
    public String updateProfile(@RequestParam String fullName,
                                @RequestParam String phone,
                                HttpServletRequest request,
                                Model model) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "redirect:/login";
        }

        // Gọi Firestore Service để cập nhật dữ liệu
        UserService.updateUserField(username, "fullName", fullName);
        UserService.updateUserField(username, "phone", phone);
        // Lấy dữ liệu mới từ Firestore để cập nhật lại model
        User updatedUser = UserService.getUserData(username);
        session.setAttribute("user", updatedUser);
        model.addAttribute("user", updatedUser);

        return "redirect:/profile"; // Trở về trang profile
    }

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
    @GetMapping("/paymentHistory")
    public String listPayments(@RequestParam(defaultValue = "0") int page, HttpServletRequest request, Model model) {
        try {
            String username = cookieService.getCookie(request, "noname");
            System.out.println("username=" + username);
            int pageSize = 10; // Số bản ghi trên mỗi trang
            Long lastTimestamp = page > 0 ? getLastTimestamp(page - 1, pageSize, username) : null;
            System.out.println(lastTimestamp);
            List<Payment> payments = userService.getPaymentsByPage(username, pageSize, lastTimestamp);
            model.addAttribute("payments", payments);
            model.addAttribute("currentPage", page);
            model.addAttribute("nextPage", page + 1);
            //System.out.println(payments);
            return "paymentHistory"; // Trả về trang payments.html
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    private Long getLastTimestamp(int page, int pageSize, String username) throws ExecutionException, InterruptedException {
        List<Payment> previousPage = userService.getPaymentsByPage(username, pageSize, null);
        if (!previousPage.isEmpty()) {
            return previousPage.get(previousPage.size() - 1).getTimestamp();
        }
        return null;
    }

}
