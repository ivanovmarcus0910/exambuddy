package com.example.exambuddy.controller;

import com.example.exambuddy.model.User;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

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
    public String oauth2LoginSuccess(@AuthenticationPrincipal OidcUser oidcUser, @AuthenticationPrincipal OAuth2User oauth2User, HttpSession session, HttpServletResponse response) throws UnsupportedEncodingException {
        String email = null;
        String name = null;
        String picture = null;

        if (oidcUser != null) { // Google
            email = oidcUser.getAttribute("email");
            name = oidcUser.getAttribute("name");
            picture = oidcUser.getAttribute("picture");
        } else if (oauth2User != null) { // Facebook
            email = oauth2User.getAttribute("email");
            name = oauth2User.getAttribute("name");
            Map<String, Object> pictureObj = oauth2User.getAttribute("picture");
            if (pictureObj != null) {
                Map<String, Object> data = (Map<String, Object>) pictureObj.get("data");
                if (data != null) {
                    picture = (String) data.get("url");
                }
            }
            System.out.println("Avatar URL: " + picture);
        }

        if (email != null) {
            UserService.saveOAuth2User(email, name, picture);

            session.setAttribute("loggedInUser", email);

            Cookie nonameCookie = new Cookie("noname", URLEncoder.encode(email, "UTF-8"));
            nonameCookie.setMaxAge(24 * 60 * 60);
            nonameCookie.setPath("/");
            response.addCookie(nonameCookie);
            session.setAttribute("urlimg", UserService.getAvatarUrlByUsername(email));

            System.out.println("Đăng nhập thành công với email: " + email);
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

    // ✅ Xử lý đăng nhập trong Spring Boot với session & cookie
    @PostMapping("/login")
    public String login(@RequestParam String username,
                        @RequestParam String password,
                        @RequestParam(required = false) boolean rememberMe,
                        Model model,
                        HttpSession session,
                        HttpServletResponse response,
                        HttpServletRequest request) throws UnsupportedEncodingException {

        if (username.isEmpty() || password.isEmpty()) {
            model.addAttribute("error", "Vui lòng nhập đầy đủ thông tin đăng nhập!");
            return "login";
        }

        // Kiểm tra xem tài khoản có tồn tại không
        User user = userService.getUserByUsername(username);
        if (user == null) {
            model.addAttribute("error", "Tài khoản không tồn tại trong hệ thống!");
            return "login";
        }
        // Kiểm tra xác thực email
        if (!authService.isEmailVerified(username)) {
            model.addAttribute("error", "Tài khoản chưa được xác thực. Vui lòng kiểm tra email.");
            model.addAttribute("actionType", "register");  // Xác thực tài khoản
            return "login";
        }

        // Kiểm tra nếu mật khẩu không đúng
        if (!authService.authenticate(username, password)) {
            model.addAttribute("error", "Mật khẩu không đúng. Vui lòng thử lại!");
            return "login";
        }
        // Xác thực tài khoản
        if (authService.authenticate(username, password)) {
            // Lấy đối tượng người dùng
            if (user == null) {
                model.addAttribute("error", "Không tìm thấy người dùng!");
                return "login";
            }
            // Kiểm tra trạng thái active của tài khoản
            if (!user.isActive()) {
                model.addAttribute("error", "Tài khoản của bạn đã bị vô hiệu hóa!");
                return "login";
            }

            // Lưu thông tin đăng nhập vào session
            session.setAttribute("loggedInUser", username);
            session.setAttribute("urlimg", UserService.getAvatarUrlByUsername(username));
            System.out.println("Người dùng đăng nhập: " + username);

            // 🔥 Thêm role vào session
            if (user != null) {
                session.setAttribute("role", user.getRole().toString()); // 🔥 Lưu role vào session
                System.out.println("Đã lưu role vào session: " + user.getRole());
            }

            // Nếu là admin thi chuyển trang
//            if (authService.isAdmin(username)) {
//                System.out.println("✅ Gọi isAdmin() thành công.");
//                return "redirect:/adminDashboard/dashboard";
//            }
            if (rememberMe) {
                cookieService.setCookie(response, "rememberedUsername", URLEncoder.encode(username, "UTF-8"));
                cookieService.setCookie(response, "rememberedPassword", URLEncoder.encode(password, "UTF-8"));
                cookieService.setCookie(response, "noname", URLEncoder.encode(username, "UTF-8"));
            } else {
                // Xoá cookie nếu không chọn "Ghi nhớ đăng nhập"
                cookieService.setCookie(response, "noname", URLEncoder.encode(username, "UTF-8"));
                cookieService.removeCookie(response, "rememberedUsername");
                cookieService.removeCookie(response, "rememberedPassword");

            }

            return "redirect:/home";
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

        //Kiểm tra email đã đăng kí trong hệ thống chưa
        if (!authService.isEmailExists(email)) {
            model.addAttribute("error", "Email chưa được đăng kí trong hệ thống!");
            return "forgotPass";
        }
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
                         @RequestParam String username,
                         @RequestParam String password,
                         @RequestParam String confirmPassword,
                         @RequestParam String role,
                         Model model) {

        boolean hasError = false;

        // Chạy kiểm tra tồn tại email và username song song
        CompletableFuture<Boolean> emailExistsFuture = CompletableFuture.supplyAsync(() -> authService.isEmailExists(email));
        CompletableFuture<Boolean> usernameExistsFuture = CompletableFuture.supplyAsync(() -> authService.isUsernameExists(username));

        try {
            boolean emailExists = emailExistsFuture.get();
            boolean usernameExists = usernameExistsFuture.get();

            if (emailExists) {
                model.addAttribute("emailError2", "Email này đã được sử dụng!");
                hasError = true;
            }
            if (usernameExists) {
                model.addAttribute("usernameError2", "Tên đăng nhập này đã tồn tại!");
                hasError = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", "Lỗi hệ thống. Vui lòng thử lại sau!");
            return "signup";
        }

        // Nếu có lỗi, quay lại trang signup + giữ lại thông tin nhập vào
        if (hasError) {
            model.addAttribute("emailValue", email);
            //model.addAttribute("phoneValue", phone);
            model.addAttribute("usernameValue", username);
            return "signup";
        }

        User.Role userRole;
        try {
            userRole = User.Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            userRole = User.Role.STUDENT;
        }

        String result = authService.registerUser(email, username, password, userRole);
        model.addAttribute("email", email);
        model.addAttribute("actionType", "register");  // Xác thực tài khoản
        model.addAttribute("message", result);
        model.addAttribute("success", "🎉 Đăng ký thành công! Kiểm tra email để xác thực tài khoản.");
        return "verifyOTP"; // Dùng chung trang verifyOTP.html
    }


}
