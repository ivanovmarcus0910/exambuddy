package com.example.exambuddy.service;

import com.example.exambuddy.model.User;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Properties;
import java.util.Random;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    private final String senderEmail = "exambuddy.team@gmail.com";
    private final String senderPassword = "tnvd pdpt axvt ozqa";

    // Thêm phương thức bất đồng bộ
    @Async("taskExecutor")
    public CompletableFuture<Void> sendEmailAsync(String recipientEmail, String subject, String message) {
        return CompletableFuture.runAsync(() -> {
            try {
                sendEmail(recipientEmail, subject, message);
            } catch (MessagingException e) {
                System.out.println("❌ Lỗi khi gửi email: " + e.getMessage());
            }
        });
    }

    public void sendEmail(String recipientEmail, String subject, String message) throws MessagingException {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "465");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.socketFactory.port", "465");
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");

        Session mailSession = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        MimeMessage mimeMessage = new MimeMessage(mailSession);
        mimeMessage.setFrom(new InternetAddress(senderEmail));
        mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
        mimeMessage.setSubject(subject);
        mimeMessage.setContent(message, "text/html; charset=UTF-8");

        Transport.send(mimeMessage);
        System.out.println("✅ Email đã được gửi đến: " + recipientEmail);
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> sendOtpEmailAccount(String recipientEmail, String otp) {
        String subject = "Mã OTP để xác thực tài khoản";
        String message = "Chào bạn,\n\nMã OTP của bạn để xác thực tài khoản là: " + otp
                + "\nMã có hiệu lực trong 60 giây.\n\nNếu bạn không xác thực tài khoản, vui lòng bỏ qua email này.";
        return sendEmailAsync(recipientEmail, subject, message);
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> sendOtpEmail(String recipientEmail, String otp) {
        String subject = "Mã OTP đặt lại mật khẩu";
        String message = "Chào bạn,\n\nMã OTP của bạn để đặt lại mật khẩu là: " + otp
                + "\nMã có hiệu lực trong 60 giây.\n\nNếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.";
        return sendEmailAsync(recipientEmail, subject, message);
    }

    @Async("taskExecutor")
    public CompletableFuture<Void> sendTeacherStatusNotification(String recipientEmail, boolean isAccepted, User user) {
        String subject = isAccepted ? "Yêu cầu của bạn đã được chấp nhận" : "Yêu cầu của bạn đã bị từ chối";
        String fullName = user.getLastName() + " " + user.getFirstName();
        String fontFamily = "font-family: Arial, Tahoma, Verdana, sans-serif;";
        String message = isAccepted ?
                "<html><body style='" + fontFamily + "'>" +
                        "<p><strong>Chào <span style='color: #4CAF50;'>" + fullName + "</span>,</strong></p>" +
                        "<p style='font-size: 16px;'>Chúng tôi vui mừng thông báo rằng yêu cầu trở thành giáo viên của bạn đã được chấp nhận. Bạn đã chính thức trở thành giáo viên của hệ thống.</p>" +
                        "<p style='font-size: 16px;'>Cảm ơn bạn đã tham gia cùng chúng tôi!</p>" +
                        "<br><p style='font-size: 16px;'>Trân trọng,<br><span style='color: #1E88E5;'>Exam Buddy Team</span></p>" +
                        "</body></html>" :
                "<html><body style='" + fontFamily + "'>" +
                        "<p><strong>Chào <span style='color: #F44336;'>" + fullName + "</span>,</strong></p>" +
                        "<p style='font-size: 16px;'>Chúng tôi rất tiếc phải thông báo rằng yêu cầu trở thành giáo viên của bạn đã bị từ chối.</p>" +
                        "<p style='font-size: 16px;'>Cảm ơn bạn đã quan tâm và chúng tôi hi vọng sẽ có cơ hội làm việc cùng bạn trong tương lai!</p>" +
                        "<br><p style='font-size: 16px;'>Trân trọng,<br><span style='color: #1E88E5;'>Exam Buddy Team</span></p>" +
                        "</body></html>";
        return sendEmailAsync(recipientEmail, subject, message);
    }

    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000);
        return String.valueOf(otp);
    }
}