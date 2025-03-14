package com.example.exambuddy.controller;

import com.example.exambuddy.service.FirebaseAuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OtpController {
    @Autowired
    private FirebaseAuthService authService;


    @GetMapping("/support-verification")
    public String showSupportVerificationPage() {
        return "supportVerification";
    }

    @PostMapping("/resendOTP")
    public String resendOtp(@RequestParam String email, @RequestParam String actionType, Model model) {
        System.out.println("📩 Đang gửi lại OTP cho email: " + email);

        if (!authService.isEmailExists(email).join()) {
            model.addAttribute("error", "Email chưa được đăng ký trong hệ thống!");
            return "supportVerification";
        }

        if (authService.isEmailVerified(email) && "register".equals(actionType)) {
            model.addAttribute("error", "Tài khoản này đã được xác thực!");
            return "supportVerification";
        }

        String result = String.valueOf(authService.resendOtp(email, actionType));
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
                model.addAttribute("success", "Tài khoản đã được xác thực! Hãy đăng nhập.");
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
}
