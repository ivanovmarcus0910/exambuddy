package com.example.exambuddy.controller;

import com.example.exambuddy.model.User;
import com.example.exambuddy.service.CookieService;
import com.example.exambuddy.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
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
    public static long generateOrderCode() {
        long timestamp = Instant.now().toEpochMilli(); // Lấy timestamp hiện tại
        int randomPart = new Random().nextInt(900) + 100; // 3 số ngẫu nhiên (100-999)
        return Long.parseLong(String.valueOf(timestamp) + randomPart);
    }

    private final PayOS payOS;
    private final CookieService cookieService = new CookieService();

    public CheckoutController(PayOS payOS) {
        super();
        this.payOS = payOS;
    }

    @RequestMapping(value = "/payment-index")
    public String Index() {
        return "createPayment";
    }

    @RequestMapping(value = "/success")
    public String Success() {

        return "success";
    }

    @RequestMapping(value = "/cancel")
    public String Cancel() {

        return "cancel";
    }

    @RequestMapping(method = RequestMethod.POST, value = "/create-payment-link", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public void checkout(HttpServletRequest request, HttpServletResponse httpServletResponse, @RequestParam("amount") int amount) {
        try {
            String username = URLDecoder.decode(cookieService.getCookie(request, "noname"));
            final String baseUrl = getBaseUrl(request);
            final String productName = "Upgrade Account";
            final String description = "Donate Point";
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
            userService.addPaymentTransaction(orderCode, amount, checkoutUrl, data.getStatus(), username);
            httpServletResponse.setHeader("Location", checkoutUrl);
            httpServletResponse.setStatus(302);
        } catch (Exception e) {
            e.printStackTrace();
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