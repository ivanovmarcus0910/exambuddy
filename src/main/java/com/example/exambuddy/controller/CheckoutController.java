package com.example.exambuddy.controller;

import com.example.exambuddy.model.User;
import com.example.exambuddy.service.CookieService;
import com.example.exambuddy.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;

import java.net.URLDecoder;
import java.time.Instant;
import java.util.Date;
import java.util.Random;

@Controller
public class CheckoutController {
    private final UserService userService;

    public static long generateOrderCode() {
        long timestamp = Instant.now().toEpochMilli(); // Lấy timestamp hiện tại
        int randomPart = new Random().nextInt(900) + 100; // 3 số ngẫu nhiên (100-999)
        return Long.parseLong(String.valueOf(timestamp) + randomPart);
    }

    private final PayOS payOS;
    private final CookieService cookieService = new CookieService();

    public CheckoutController(PayOS payOS, UserService userService) {
        super();
        this.payOS = payOS;
        this.userService = userService;
    }

    @RequestMapping(value = "/payment-coin")
    public String CreatePayment(HttpServletRequest request, HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/home"; // Nếu chưa đăng nhập, chuyển hướng về home
        }
        String username = session.getAttribute("loggedInUser").toString();
        User user =userService.getUserByUsername(username);
        model.addAttribute("user", user);

        return "createPayment";
    }

    @RequestMapping(value = "/success")
    public String Success() {

        return "success";
    }

    @RequestMapping(value = "/cancel")
    public String cancelTransaction(@RequestParam("orderCode") String paymentCode) {
        boolean isUpdated = userService.updatePaymentStatusFail(Long.parseLong(paymentCode), "CANCELLED");
        return "cancel"; // Quay lại danh sách thanh toán
    }
    @RequestMapping(method = RequestMethod.POST, value = "/create-payment-link", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void checkout(HttpServletRequest request, HttpServletResponse httpServletResponse, @RequestParam("amount") int amount, HttpSession session) {
        try {
            String username = session.getAttribute("loggedInUser").toString();
            final String baseUrl = getBaseUrl(request);
            final String productName = "Coin";
            final String description = "Deposit Coin";
            final String returnUrl = baseUrl + "/success";
            final String cancelUrl = baseUrl + "/cancel";
            final int price = amount;
            // Gen order code
            long orderCode = generateOrderCode();
            ItemData item = ItemData.builder().name(productName).quantity(1).price(price).build();
            PaymentData paymentData = PaymentData.builder().orderCode(orderCode).amount(price).description(description)
                    .returnUrl(returnUrl).cancelUrl(cancelUrl).item(item).build();

            CheckoutResponseData data = payOS.createPaymentLink(paymentData);

            String checkoutUrl = data.getCheckoutUrl();
            UserService userService = new UserService();
            userService.addPaymentTransaction(orderCode, amount, checkoutUrl, data.getStatus(), username, "Deposit");
            httpServletResponse.setHeader("Location", checkoutUrl);
            httpServletResponse.setStatus(302);
        } catch (Exception e) {
            try {
                request.setAttribute("error", "Payment gateway is currently unavailable. Please try again later."); // Truyền thông tin lỗi
                request.getRequestDispatcher("error").forward(request, httpServletResponse);
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private String getBaseUrl(HttpServletRequest request) {
        String scheme = request.getScheme();
        String serverName = request.getServerName();
        int serverPort = request.getServerPort();
        String contextPath = request.getContextPath();

        String url = scheme + "://" + serverName;
        if ((scheme.equals("http") && serverPort != 80) || (scheme.equals("https") && serverPort != 443)) {
            url += ":" + serverPort;
        }
        url += contextPath;
        return url;
    }
}