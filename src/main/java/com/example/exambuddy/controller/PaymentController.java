package com.example.exambuddy.controller;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import vn.payos.PayOS;
import vn.payos.type.CheckoutResponseData;
import vn.payos.type.ItemData;
import vn.payos.type.PaymentData;

import java.io.IOException;

@Controller
public class PaymentController {

    String clientId = "aae60675-76d3-4fdf-897e-c6d2c8f4207f";
    String apiKey = "fabf143e-0d7b-4712-93e2-be7014ee2f39";
    String checksumKey = "780d39bae9a6bb0a1937fcd4d3e26e6eb8b4e3e504cf942df450daeaba763576";

    final PayOS payOS = new PayOS(clientId, apiKey, checksumKey);
    @GetMapping("/payment")
    public String index(){
        return "index";
    }
    @PostMapping("/creatPayment")
    public String createPayment(@RequestParam String orderId, @RequestParam long amount, Model model) throws IOException {
        String domain = "http://localhost:8080";
        Long orderCode = System.currentTimeMillis() / 1000;
        ItemData itemData = ItemData
                .builder()
                .name("Mỳ tôm Hảo Hảo ly")
                .quantity(1)
                .price(2000)
                .build();

        PaymentData paymentData = PaymentData
                .builder()
                .orderCode(orderCode)
                .amount(2000)
                .description("Thanh toán đơn hàng")
                .returnUrl(domain + "/payment-success.html")
                .cancelUrl(domain + "/payment-cancel.html")
                .item(itemData)
                .build();
        CheckoutResponseData result;
        try {
             result = payOS.createPaymentLink(paymentData);
        return "redirect:" + result.getCheckoutUrl();
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return "";

    }

}
