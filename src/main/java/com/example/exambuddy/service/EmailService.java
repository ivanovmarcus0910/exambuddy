package com.example.exambuddy.service;

import com.example.exambuddy.model.User;
import org.springframework.stereotype.Service;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;
import java.util.Random;

@Service
public class EmailService {

    /*
    private final String senderEmail = "apartmentprovjp@gmail.com"; // Email gửi
    private final String senderPassword = "uzvr cjwb oxna xmbu" ; // Mật khẩu ứng dụng (App Password)

     */
    private final String senderEmail = "exambuddy.team@gmail.com"; // Email gửi
    private final String senderPassword = "tnvd pdpt axvt ozqa" ; // Mật khẩu ứng dụng (App Password)

    public String generateOtp() {
        Random random = new Random();
        int otp = 100000 + random.nextInt(900000); // OTP 6 chữ số
        return String.valueOf(otp);
    }
    /**
     * Gửi email xác thực tài khoản
     * @param recipientEmail Email của người dùng cần xác thực
     * @param token Mã xác thực duy nhất
     */
    /*
    public void sendVerificationEmail(String recipientEmail, String token) throws MessagingException {
        String subject = "Xác thực tài khoản E-Learning của bạn";
        String verificationUrl = "OTP : " + token;
        String message = "Chào bạn,\n\nVui lòng nhập mã OTP sau để xác thực tài khoản của bạn:\n"
                + verificationUrl + "\n\nCảm ơn bạn đã đăng ký!";

        sendEmail(recipientEmail, subject, message);
    }
    */

    /**
     * Gửi email chứa mã OTP để đặt lại mật khẩu
     */
    public void sendOtpEmail(String recipientEmail, String otp) throws MessagingException {
        String subject = "Mã OTP đặt lại mật khẩu";
        String message = "Chào bạn,\n\nMã OTP của bạn để đặt lại mật khẩu là: " + otp
                + "\nMã có hiệu lực trong 60 giây.\n\nNếu bạn không yêu cầu đặt lại mật khẩu, vui lòng bỏ qua email này.";

        sendEmail(recipientEmail, subject, message);
    }

    public void sendOtpEmailAccount(String recipientEmail, String otp) throws MessagingException {
        String subject = "Mã OTP để xác thực tài khoản";
        String message = "Chào bạn,\n\nMã OTP của bạn để xác thực tài khoản là: " + otp
                + "\nMã có hiệu lực trong 60 giây.\n\nNếu bạn không xác thực tài khoản, vui lòng bỏ qua email này.";

        sendEmail(recipientEmail, subject, message);
    }

    /**
     * Cấu hình và gửi email qua SMTP với SSL (Port 465)
     *
     * @param recipientEmail Địa chỉ email nhận
     * @param subject        Chủ đề email
     * @param message        Nội dung email
     */
    public void sendEmail(String recipientEmail, String subject, String message) throws MessagingException {
        // Cấu hình SMTP với SSL (Port 465)
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com"); // Máy chủ SMTP của Gmail
        props.put("mail.smtp.port", "465"); // Cổng 465 sử dụng SSL
        props.put("mail.smtp.auth", "true"); // Yêu cầu xác thực
        props.put("mail.smtp.socketFactory.port", "465"); // Xác định cổng SSL
        props.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory"); // Sử dụng SSL

        // Xác thực tài khoản Gmail
        Session mailSession = Session.getDefaultInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(senderEmail, senderPassword);
            }
        });

        try {
            // Tạo email
            MimeMessage mimeMessage = new MimeMessage(mailSession);
            mimeMessage.setFrom(new InternetAddress(senderEmail));
            mimeMessage.addRecipient(Message.RecipientType.TO, new InternetAddress(recipientEmail));
            mimeMessage.setSubject(subject);
            mimeMessage.setContent(message, "text/html; charset=UTF-8");


            // Gửi email
            Transport.send(mimeMessage);
            System.out.println("✅ Email xác thực đã được gửi đến: " + recipientEmail);

        } catch (MessagingException e) {
            System.out.println("❌ Lỗi khi gửi email xác thực: " + e.getMessage());
            throw e;  // Đẩy lỗi lên để FirebaseAuthService xử lý
        }
    }
    public void sendTeacherStatusNotification(String recipientEmail, boolean isAccepted, User user) throws MessagingException {
        String subject = isAccepted ? "Yêu cầu của bạn đã được chấp nhận" : "Yêu cầu của bạn đã bị từ chối";
        String message;

        // Sử dụng họ tên người dùng từ đối tượng User
        String fullName = user.getLastName() +" "+ user.getFirstName(); // Giả sử bạn có phương thức này trong lớp User
        String fontFamily = "font-family: Arial, Tahoma, Verdana, sans-serif;";
        if (isAccepted) {
            message = "<html><body style='" + fontFamily + "'>" +
                    "<p><strong>Chào <span style='color: #4CAF50;'>" + fullName + "</span>,</strong></p>" +  // Chỉ fullname có màu
                    "<p style='font-size: 16px;'>Chúng tôi vui mừng thông báo rằng yêu cầu trở thành giáo viên của bạn đã được chấp nhận. Bạn đã chính thức trở thành giáo viên của hệ thống.</p>" +
                    "<p style='font-size: 16px;'>Cảm ơn bạn đã tham gia cùng chúng tôi!</p>" +
                    "<br><p style='font-size: 16px;'>Trân trọng,<br><span style='color: #1E88E5;'>Exam Buddy Team</span></p>" +
                    "</body></html>";
        } else {
            message = "<html><body style='" + fontFamily + "'>" +
                    "<p><strong>Chào <span style='color: #F44336;'>" + fullName + "</span>,</strong></p>" +  // Chỉ fullname có màu
                    "<p style='font-size: 16px;'>Chúng tôi rất tiếc phải thông báo rằng yêu cầu trở thành giáo viên của bạn đã bị từ chối.</p>" +
                    "<p style='font-size: 16px;'>Cảm ơn bạn đã quan tâm và chúng tôi hi vọng sẽ có cơ hội làm việc cùng bạn trong tương lai!</p>" +
                    "<br><p style='font-size: 16px;'>Trân trọng,<br><span style='color: #1E88E5;'>Exam Buddy Team</span></p>" +
                    "</body></html>";
        }



        // Gửi email thông báo cho người dùng
        sendEmail(recipientEmail, subject, message);
    }


    /**
     * Tạo mã OTP ngẫu nhiên gồm 6 chữ số
     */

}
