package com.example.exambuddy.controller;

import com.example.exambuddy.service.CookieService;
import com.example.exambuddy.service.FirebaseAuthService;
import com.example.exambuddy.service.PasswordService;
import com.example.exambuddy.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

@Controller
public class AuthController {

    @Autowired
    private FirebaseAuthService authService;
    @Autowired
    private PasswordService passService;
    @Autowired
    private UserService userService;

    @Autowired
    private CookieService cookieService;
    //Tra ve trang login
    @GetMapping("/login")
    public String showLoginPage(HttpSession session) {
        if (session.getAttribute("loggedInUser") != null) {
            return "redirect:/home"; // Nếu đã đăng nhập, chuyển hướng về home
        }
        return "login"; // Nếu chưa đăng nhập, hiển thị trang login
    }

    // Xử lý đăng nhập OAuth2
    @GetMapping("/oauth2/success")
    public String oauth2LoginSuccess(@AuthenticationPrincipal OidcUser oidcUser, HttpSession session, HttpServletResponse response) throws UnsupportedEncodingException {
        if (oidcUser != null) {
            String email = oidcUser.getAttribute("email");
            String name = oidcUser.getAttribute("name");
            String picture = oidcUser.getAttribute("picture");

            // Lưu thông tin vào Firestore nếu chưa có
            UserService.saveOAuth2User(email, name, picture);

            session.setAttribute("loggedInUser", email);

            // Thêm cookie noname để thống nhất với đăng nhập thường
            Cookie nonameCookie = new Cookie("noname", URLEncoder.encode(email, "UTF-8"));
            nonameCookie.setMaxAge(24 * 60 * 60);
            nonameCookie.setPath("/");
            response.addCookie(nonameCookie);
            System.out.println("Đăng nhập Google thành công với email: " + email);;
            return "redirect:/home";
        }
        return "redirect:/login";
    }

    @GetMapping("/oauth2/failure")
    public String oauth2LoginFailure(Model model) {
        model.addAttribute("error", "Đăng nhập bằng Google/Facebook thất bại!");
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
                cookieService.setCookie(response, "rememberedUsername", URLEncoder.encode(username, "UTF-8"));
                cookieService.setCookie(response, "rememberedPassword", URLEncoder.encode(password, "UTF-8"));
                cookieService.setCookie(response, "noname", URLEncoder.encode(username, "UTF-8"));
            } else {
                // Xoá cookie nếu không chọn "Ghi nhớ đăng nhập"
                cookieService.setCookie(response, "noname", URLEncoder.encode(username, "UTF-8"));
                cookieService.removeCookie(response,"rememberedUsername");
                cookieService.removeCookie(response,"rememberedPassword");

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
        cookieService.removeCookie(response,"rememberedUsername");
        cookieService.removeCookie(response,"rememberedPassword");
        cookieService.removeCookie(response,"noname");
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
        boolean isUpdated = passService.updatePassword(email, newPassword);
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

        // Mã hoá pass trước khi lưu và db
        //String hashPass = passService.encodePassword(password);


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
