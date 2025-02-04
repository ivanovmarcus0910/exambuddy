package com.example.exambuddy.controller;

import com.example.exambuddy.service.FirebaseAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
public class AuthController {

    @Autowired
    private FirebaseAuthService authService;

    //Tra ve trang login
    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    // Điều hướng trang Home
    @GetMapping("")
    public String homePage() {
        return "home";
    }

    // Xử lý đăng nhập
    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password, Model model) {
        if (!authService.isEmailVerified(username)) {
            model.addAttribute("error", "Tài khoản chưa được xác thực. Vui lòng kiểm tra email.");
            return "login";
        }
        System.out.println(authService.authenticate(username,password));
        if (authService.authenticate(username, password)) {
            return "home";
        }
        model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
        return "login";
    }


    @GetMapping("/forgotPass")
    public String forgotPasswordPage() {
        return "forgotPass";
    }

    @PostMapping("/forgotPass")
    public String sendOtp(@RequestParam String email, Model model) {
        System.out.println("📩 Đã nhận yêu cầu gửi OTP cho email: " + email);
        String result = authService.sendPasswordResetOtp(email);
        model.addAttribute("message", result);
        model.addAttribute("email", email);
        return "verifyOTP";
    }

    @PostMapping("/resendOTP")
    public String resendOtp(@RequestParam String email, @RequestParam String actionType, Model model) {
        System.out.println("📩 Đang gửi lại OTP cho email: " + email);
        String result = authService.resendOtp(email, actionType);
        model.addAttribute("message", result);
        model.addAttribute("email", email);
        model.addAttribute("actionType", actionType);

        return "verifyOTP";
    }


    @PostMapping("/verifyOTP")
    public String verifyOtp(@RequestParam String email, @RequestParam String otp,
                            @RequestParam String actionType, Model model) {
        boolean success;

        if ("register".equals(actionType)) {
            success = authService.verifyAccountOtp(email, otp);
        } else {
            success = authService.verifyOtp(email, otp);
        }

        if (success) {
            if ("register".equals(actionType)) {
                model.addAttribute("message", "Tài khoản đã được xác thực! Hãy đăng nhập.");
                return "login";
            } else {
                model.addAttribute("email", email);
                return "resetPass";
            }
        } else {
            model.addAttribute("error", "Mã OTP không hợp lệ hoặc đã hết hạn.");
            model.addAttribute("email", email);
            model.addAttribute("actionType", actionType);
            return "verifyOTP";
        }
    }

    @GetMapping("/resetPass")
    public String ressetPassPage() {
        return "resetPass";
    }

    // Xử lý đặt lại mật khẩu
    @PostMapping("/resetPass")
    public String resetPassword(@RequestParam String email,
                                @RequestParam String newPassword,
                                @RequestParam String confirmPassword,
                                Model model) {
        // Kiểm tra xác nhận mật khẩu
        if (!newPassword.equals(confirmPassword)) {
            model.addAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "resetPass";
        }

        // Cập nhật mật khẩu
        boolean isUpdated = authService.updatePassword(email, newPassword);
        if (isUpdated) {
            model.addAttribute("success", "Mật khẩu đã cập nhật thành công! Hãy đăng nhập lại.");
            return "login";  // Chuyển về trang đăng nhập
        } else {
            model.addAttribute("error", "Lỗi khi cập nhật mật khẩu. Vui lòng thử lại!");
            return "resetPass";
        }
    }

    // Tra ve trang signup
    @GetMapping("/signup")
    public String signupPage() {
        return "signup";
    }

    // Xu li signup
    @PostMapping("/signup")
    public String signup(@RequestParam String email,
                         @RequestParam String phone,
                         @RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String confirmPassword,
                         Model model) {

        boolean hasError = false;
        // Kiểm tra nếu bất kỳ trường nào bị thiếu
        if (email == null || phone == null || username == null || password == null || confirmPassword == null) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ thông tin!");
            hasError = true;
        }

        // Kiểm tra email hợp lệ
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            model.addAttribute("emailError", "Email không hợp lệ!");
            hasError = true;
        }

        // Kiểm tra số điện thoại hợp lệ (tối thiểu 9 số)
        if (!phone.matches("^\\d{9,}$")) {
            model.addAttribute("phoneError", "Số điện thoại không hợp lệ!");
            hasError = true;
        }

        // Kiểm tra mật khẩu xác nhận
        if (!password.equals(confirmPassword)) {
            model.addAttribute("passwordError", "Mật khẩu xác nhận không khớp!");
            hasError = true;
        }

        // Kiểm tra xem email đã tồn tại chưa
        if (authService.isEmailExists(email)) {
            model.addAttribute("emailError", "Email này đã được sử dụng!");
            hasError = true;
        }

        // Kiểm tra xem username đã tồn tại chưa
        if (authService.isUsernameExists(username)) {
            model.addAttribute("usernameError", "Tên đăng nhập này đã tồn tại!");
            hasError = true;
        }

        // Nếu có lỗi, quay lại trang signup + giữ lại thông tin nhập vào
        if (hasError) {
            model.addAttribute("emailValue", email);
            model.addAttribute("phoneValue", phone);
            model.addAttribute("usernameValue", username);
            return "signup";
        }

        /**
         * Đăng ký và gửi email xác thực
         */
        /*
        System.out.println("👉 Đăng ký người dùng: " + email);
        String result = authService.registerUser(email, phone, username, password);
        if (result.startsWith("Error")) {
            model.addAttribute("error", result);
            return "signup";
        }
        System.out.println("👉 Gửi email xác thực với token: " + result);

        // Chuyển đến trang register.html
        model.addAttribute("email", email);
        return "register.html";

         */
        String result = authService.registerUser(email, phone, username, password);
        model.addAttribute("email", email);
        model.addAttribute("actionType", "register");  // Xác thực tài khoản
        model.addAttribute("message", result);
        return "verifyOTP"; // Dùng chung trang verifyOTP.html
    }
}
