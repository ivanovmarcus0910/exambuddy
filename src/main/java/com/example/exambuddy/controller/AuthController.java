package com.example.exambuddy.controller;

import com.example.exambuddy.service.FirebaseAuthService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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

    // ✅ Xử lý đăng nhập trong Spring Boot với session & cookie
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam(required = false) boolean rememberMe,
                        Model model,
                        HttpSession session,
                        HttpServletResponse response,
                        HttpServletRequest request) throws UnsupportedEncodingException {

        // Kiểm tra xác thực email
        if (!authService.isEmailVerified(username)) {
            model.addAttribute("error", "Tài khoản chưa được xác thực. Vui lòng kiểm tra email.");
            return "login";
        }

        // Xác thực tài khoản
        if (authService.authenticate(username, password)) {
            // Lưu thông tin đăng nhập vào session
            session.setAttribute("loggedInUser", username);


            if (rememberMe) {
                Cookie usernameCookie = new Cookie("rememberedUsername", URLEncoder.encode(username, "UTF-8"));
                Cookie passwordCookie = new Cookie("rememberedPassword", URLEncoder.encode(password, "UTF-8"));
                Cookie noname = new Cookie("noname", URLEncoder.encode(username, "UTF-8"));

                usernameCookie.setMaxAge(24 * 60 * 60); // Lưu trong 7 ngày
                passwordCookie.setMaxAge(24 * 60 * 60);
                noname.setMaxAge(24 * 60 * 60/7);
                usernameCookie.setHttpOnly(false); // Cho phép truy cập từ JavaScript
                passwordCookie.setHttpOnly(false);
                noname.setHttpOnly(false);
                usernameCookie.setSecure(false); // Cho phép trên HTTP
                passwordCookie.setSecure(false);
                noname.setSecure(false);
                usernameCookie.setPath("/");
                passwordCookie.setPath("/");
                noname.setPath("/");
                response.addCookie(usernameCookie);
                response.addCookie(passwordCookie);
                response.addCookie(noname);
            } else {
                // Xoá cookie nếu không chọn "Ghi nhớ đăng nhập"
                Cookie[] cookies = request.getCookies();
                Cookie noname = new Cookie("noname", URLEncoder.encode(username, "UTF-8"));
                noname.setMaxAge(24 * 60 * 60/7);
                noname.setHttpOnly(false);
                noname.setSecure(false);
                response.addCookie(noname);

                if (cookies != null) {
                    for (Cookie cookie : cookies) {
                        if ("rememberedUsername".equals(cookie.getName()) || "rememberedPassword".equals(cookie.getName())) {
                            cookie.setMaxAge(0);
                            cookie.setPath("/");
                            response.addCookie(cookie);
                        }
                    }
                }
            }
            return "redirect:/home";
        }
        model.addAttribute("error", "Tên đăng nhập hoặc mật khẩu không đúng!");
        return "login";
    }




    @RequestMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response, HttpServletRequest request) {
        session.removeAttribute("loggedInUser");
        session.invalidate();
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("rememberedUsername".equals(cookie.getName()) || "rememberedPassword".equals(cookie.getName()) || "noname".equals(cookie.getName())) {
                    cookie.setMaxAge(0);
                    cookie.setPath("/");
                    response.addCookie(cookie);
                }
            }
        }
        System.out.println("Đã logout");
        return "home"; // Chuyển hướng về trang home, đảm bảo session đã bị xóa
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
    public String resetPassPage(@RequestParam(required = false) String email,
                                HttpSession session, Model model) {
        if (email != null) {
            // Trường hợp đặt lại mật khẩu (quên mật khẩu)
            model.addAttribute("email", email);
        } else {
            // Trường hợp đổi mật khẩu từ trang cá nhân
            String username = (String) session.getAttribute("loggedInUser");
            if (username == null) {
                return "redirect:/login"; // Chưa đăng nhập thì chuyển về login
            }
            model.addAttribute("email", username); // Dùng username làm email
        }
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
        boolean isUpdated = authService.updatePasswordForUser(username, currentPassword, newPassword);
        if (isUpdated) {
            model.addAttribute("success", "Mật khẩu đã cập nhật thành công!");
        } else {
            model.addAttribute("error", "Mật khẩu hiện tại không đúng hoặc có lỗi khi cập nhật!");
        }

        return "changePass";
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
