package com.example.exambuddy.controller;

import com.example.exambuddy.model.Payment;
import com.example.exambuddy.model.User;
import com.example.exambuddy.service.*;
import jakarta.servlet.http.HttpServletRequest;
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
import java.util.List;
import java.util.Map;
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

//    @RequestMapping("/profile")
//    public String profilePage(HttpServletRequest request, Model model) {
//        HttpSession session = request.getSession();
//        String username = (String) session.getAttribute("loggedInUser");
//
//        if (username == null) {
//            return "redirect:/login";
//        }
//
//        // Lấy thông tin người dùng từ dịch vụ với username đã tìm thấy
//        UserService userService = new UserService();
//        User user = userService.getUserData(username);
//        model.addAttribute("user", user);
//
//        return "profile";
//    }

    @PostMapping("/profile/upload")
    public String uploadAvatar(@RequestParam("image") MultipartFile file,
                               @RequestParam String username,
                               HttpSession session,
                               Model model) throws IOException {
        String url = this.cloudinaryService.upLoadImgAvt(file, "imgAvatar", username);
        System.out.println("URL=" + url);
        UserService.changeAvatar(username, url);
        session.setAttribute("urlimg", url);
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
    public String listPayments(@RequestParam(defaultValue = "0") int page, HttpServletRequest request, Model model) {
        try {
            String username = cookieService.getCookie(request, "noname");
            System.out.println("username=" + username);
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

    @GetMapping("/upgrade")
    public String upgrage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return "redirect:/login";
        }
        User user = userService.getUserByUsername(username);
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
                    return "redirect:/profile";
                }
                else
                {

                    model.addAttribute("err", "Lỗi giao dịch, vui lòng thử lại!");
                    return "upgrade";
                }

            }
            else
            {
                model.addAttribute("err", "Lỗi giao dịch, vui lòng thử lại!");
                return "upgrade";

            }
        }
        else{
            model.addAttribute("err", "Không đủ coin vui lòng nạp và thử lại!");
            return "upgrade";
        }

    }

}
