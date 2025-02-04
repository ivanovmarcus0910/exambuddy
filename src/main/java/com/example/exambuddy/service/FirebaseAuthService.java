package com.example.exambuddy.service;

import com.example.exambuddy.model.User;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseAuthService {
    @Autowired
    private  EmailService emailService;
    private static final String COLLECTION_NAME = "users";
    private static final String OTP_COLLECTION = "password_reset_otps";

    // Đăng ký người dùng mới và gửi email xác thực
    public String registerUser(String email, String phone, String username, String password) {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference users = firestore.collection(COLLECTION_NAME);

        String verificationToken = UUID.randomUUID().toString();
        User user = new User(null, email, phone, username, password, verificationToken);

        try {
            // ✅ Gửi email xác thực trước khi lưu tài khoản
            System.out.println("📧 Gửi email xác thực đến: " + email);
            emailService.sendVerificationEmail(email, verificationToken);

            // ✅ Chỉ lưu tài khoản nếu email gửi thành công
            users.document(username).set(user);
            System.out.println("✅ Tài khoản đã được lưu vào Firestore: " + username);
            return verificationToken;

        } catch (Exception e) {
            System.out.println("❌ Lỗi khi gửi email xác thực: " + e.getMessage());
            e.printStackTrace();
            return "Error: Không thể gửi email xác thực!";
        }
    }

    //Xác thực email bằng token
    public boolean verifyEmail(String token) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("verificationToken", token);
            QuerySnapshot querySnapshot = query.get().get();

            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                DocumentReference userRef = doc.getReference();

                // Cập nhật trạng thái xác thực
                userRef.update("verified", true, "verificationToken", null);
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Gửi OTP đặt lại mật khẩu và lưu vào Firestore
     */
    public String sendPasswordResetOtp(String email) {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference otpCollection = firestore.collection(OTP_COLLECTION);

        String otp = emailService.generateOtp();
        long expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5); // Hết hạn sau 5 phút

        try {
            otpCollection.document(email).set(new OtpRecord(otp, expiryTime));
            emailService.sendOtpEmail(email, otp);
            return "OTP đã được gửi đến email.";
        } catch (Exception e) {
            return "Lỗi khi gửi OTP: " + e.getMessage();
        }
    }

    /**
    Gửi lại mã OTP khác
     */
    public String resendOtp(String email) {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference otpCollection = firestore.collection("password_reset_otps");

        String newOtp = emailService.generateOtp();
        long expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);

        try {
            DocumentSnapshot existingOtp = otpCollection.document(email).get().get();

            if (existingOtp.exists()) {
                otpCollection.document(email).update("otp", newOtp, "expiryTime", expiryTime);
            } else {
                otpCollection.document(email).set(new OtpRecord(newOtp, expiryTime));
            }

            emailService.sendOtpEmail(email, newOtp);
            return "Mã OTP mới đã được gửi!";
        } catch (Exception e) {
            return "Lỗi khi gửi lại OTP: " + e.getMessage();
        }
    }

    /**
     * Xác thực OTP trước khi cho phép đặt lại mật khẩu
     */
    public boolean verifyOtp(String email, String otp) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot otpSnapshot = firestore.collection(OTP_COLLECTION).document(email).get().get();
            if (!otpSnapshot.exists()) {
                return false;
            }

            OtpRecord otpRecord = otpSnapshot.toObject(OtpRecord.class);
            if (otpRecord == null || System.currentTimeMillis() > otpRecord.getExpiryTime()) {
                return false;
            }

            return otpRecord.getOtp().equals(otp);
        } catch (Exception e) {
            return false;
        }
    }

    static class OtpRecord {
        private String otp;
        private long expiryTime;

        public OtpRecord() {}

        public OtpRecord(String otp, long expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
        }

        public String getOtp() {
            return otp;
        }

        public long getExpiryTime() {
            return expiryTime;
        }
    }

    //Kiểm tra xem email đã xác thực chưa
    public boolean isEmailVerified(String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot userSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();
            return userSnapshot.exists() && userSnapshot.getBoolean("verified");
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean updatePassword(String email, String newPassword) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            // Tìm người dùng theo email
            Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("email", email);
            QuerySnapshot querySnapshot = query.get().get();

            if (!querySnapshot.isEmpty()) {
                DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                DocumentReference userRef = doc.getReference();

                // Cập nhật mật khẩu mới
                userRef.update("password", newPassword);
                System.out.println("✅ Mật khẩu của " + email + " đã được cập nhật!");

                // Xóa OTP sau khi cập nhật mật khẩu thành công
                firestore.collection(OTP_COLLECTION).document(email).delete();

                return true;
            } else {
                System.out.println("❌ Không tìm thấy tài khoản có email: " + email);
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Lỗi khi cập nhật mật khẩu: " + e.getMessage());
        }
        return false;
    }


    // Xác thực đăng nhập bằng username & password
    public boolean authenticate(String username, String password) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot userSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();
            if (!userSnapshot.exists()) {
                return false;
            }
            User user = userSnapshot.toObject(User.class);
            return user != null && user.getPassword().equals(password);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Kiểm tra xem email đã tồn tại chưa
    public boolean isEmailExists(String email) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("email", email);
            QuerySnapshot querySnapshot = query.get().get();
            return !querySnapshot.isEmpty();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Kiểm tra xem username đã tồn tại chưa
    public boolean isUsernameExists(String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot userSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();
            return userSnapshot.exists();
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}
