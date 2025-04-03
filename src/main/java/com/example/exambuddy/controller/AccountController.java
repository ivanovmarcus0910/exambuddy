package com.example.exambuddy.controller;

import com.example.exambuddy.model.Payment;
import com.example.exambuddy.model.User;
import com.example.exambuddy.service.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.http.HttpRequest;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

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
    private long[] chargeLevel ={25000, 150000, 300000};
    public AccountController(PasswordService passService) {
        this.passService = passService;
    }
        public boolean isImageFile(MultipartFile file) {
    String contentType = file.getContentType();
    return contentType != null && contentType.startsWith("image/");
        }
    public static long generateOrderCode() {
        long timestamp = Instant.now().toEpochMilli(); // Lấy timestamp hiện tại
        int randomPart = new Random().nextInt(900) + 100; // 3 số ngẫu nhiên (100-999)
        return Long.parseLong(String.valueOf(timestamp) + randomPart);
    }
    @PostMapping("/profile/upload")
    public String uploadAvatar(@RequestParam("image") MultipartFile file,
                               @RequestParam String username,
                               HttpSession session,
                               RedirectAttributes redirectAttributes,
                               Model model) throws IOException {
        if (isImageFile(file)) {

            String url = this.cloudinaryService.upLoadImgAvt(file, "imgAvatar", username);
            System.out.println("URL=" + url);
            if (url != null) {
                UserService.changeAvatar(username, url);
                session.setAttribute("urlimg", url);
                redirectAttributes.addFlashAttribute("success", "Cập nhật thành công!");

            } else {
                redirectAttributes.addFlashAttribute("messageUpload", "Database update failure!");

            }
        }
        else {
            redirectAttributes.addFlashAttribute("messageUpload", "Invalid file type!");

        }
        User user = UserService.getUserData(username);
        model.addAttribute("user", user);


        return "redirect:/profile";  // Trở về trang profile
    }


    @GetMapping("/changePass")
    public String changePasswordPage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về login
        }
        return "redirect:/profile"; // Trả về trang HTML để đổi mật khẩu
    }

    @PostMapping("/changePass")
    public String changePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes redirectAttributes) {

        // Lấy username của người dùng đã đăng nhập từ session
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về login
        }

        // Kiểm tra xác nhận mật khẩu mới
        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp!");
            return "redirect:/profile";
        }

        System.out.println("📌 Đang thực hiện đổi mật khẩu cho username: " + username);

        // Gọi passService để cập nhật mật khẩu
        boolean isUpdated = passService.updatePasswordForUser(username, currentPassword, newPassword);
        if (isUpdated) {
            redirectAttributes.addFlashAttribute("success", "Mật khẩu đã cập nhật thành công!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không đúng hoặc có lỗi khi cập nhật!");
        }

        return "redirect:/profile";
    }


    @GetMapping("/paymentHistory")
    public String listPayments(@RequestParam(defaultValue = "0") int page, HttpServletRequest request, Model model, HttpSession session, HttpServletResponse httpServletResponse) {
        try {
            String username = session.getAttribute("loggedInUser").toString();
            User user = userService.getUserByUsername(username);
            model.addAttribute("user", user);
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
            model.addAttribute("error", "An error occurred while retrieving transaction history. Please try again later."); // Truyền thông tin lỗi
            return "error";
        }
    }
    private Long getLastTimestamp(int page, int pageSize, String username) throws ExecutionException, InterruptedException {
        List<Payment> previousPage = userService.getPaymentsByPage(username, pageSize, null);
        if (!previousPage.isEmpty()) {
            return previousPage.get(previousPage.size() - 1).getTimestamp();
        }
        return null;
    }

    @RequestMapping("/upgrade")
    public String upgrage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        System.out.println("username=" + username);
        if (username == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByUsername(username);
        model.addAttribute("user", user);
        if (user != null &&( (user.getRole() == User.Role.STUDENT) || (user.getRole() == User.Role.UPGRADED_STUDENT))) {
            model.addAttribute("level", chargeLevel);
            return "upgrade";
        }
        else
            return "error";
    }
    @PostMapping("/upgrade")
    public  String upgradePremium(@RequestParam int plan, RedirectAttributes redirectAttributes, HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByUsername(username);
        long charge = 0, timer = 0;
        switch (plan) {
            case 1-> {
                charge=chargeLevel[0];
                timer=30L*24*60*60*1000;
            }
            case 2-> {
                charge=chargeLevel[1];
                timer=7*30L*24*60*60*1000;

            }
            case 3-> {
                charge=chargeLevel[2];
                timer=15*30L*24*60*60*1000;
            }
        }
        if (user.getCoin()>charge) {
            if (userService.changeCoinBalance(-charge, username))
            {
                if (userService.updateUserPremium(username, timer))
                {
                    userService.addPaymentTransaction(generateOrderCode(), -charge, "", "PAID", username, "Upgrade Account");

                    model.addAttribute("user", user);
                    return "redirect:/profile";
                }
                else
                {

                    redirectAttributes.addFlashAttribute("err", "Your account upgrade was unsuccessful.Please try again later!");
                    return "redirect:/upgrade";
                }

            }
            else
            {
                redirectAttributes.addFlashAttribute("err", "We are unable to check your account balance at the moment. Please try again later!");
                return "redirect:/upgrade";

            }
        }
        else{
            redirectAttributes.addFlashAttribute("err", "Your account balance is not enough. Please try again later!");
            return "redirect:/upgrade";
        }

    }
    @RequestMapping("/accountbalance")
    public String accountBalance(HttpSession session, Model model, RedirectAttributes redirectAttributes) {
        String username = (String) session.getAttribute("loggedInUser");
        System.out.println("username=" + username);
        if (username == null) {
            return "redirect:/login";
        }
        long income = 0, expenses = 0;
        try {
            int pageSize = 10; // Số bản ghi trên mỗi trang
            Long lastTimestamp = getLastTimestamp(1, pageSize, username);
            System.out.println(lastTimestamp);
            List<Payment> payments = userService.getPayments(username);
            for (Payment payment : payments) {
                if (payment.getAmount() > 0) {
                    income+=payment.getAmount();
                }
                else
                    expenses+=payment.getAmount();
            }
        }
        catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "We are unable to check your account balance at the moment. Please try again later!");

        }
        User user = userService.getUserByUsername(username);
        model.addAttribute("income", income);
        model.addAttribute("expenses", expenses);
        model.addAttribute("totalBalance", user.getCoin());
        model.addAttribute("user", user);

            return "accountbalance";

    }
}
