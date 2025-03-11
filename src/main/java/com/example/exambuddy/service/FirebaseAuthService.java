package com.example.exambuddy.service;

import com.example.exambuddy.model.User;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
public class FirebaseAuthService {
    @Autowired
    private EmailService emailService;
    @Autowired
    private PasswordService passService;
    private static final String COLLECTION_NAME = "users";
    private static final String ACCOUNT_OTP_COLLECTION = "account_verify_otps"; // xac thuc account
    private static final String OTP_COLLECTION = "password_reset_otps"; // quen pass

    // Danh sách email là admin
    private static final List<String> ADMIN_Email = Arrays.asList(
            "trungtqde180411@fpt.edu.vn",
            "hainlde180364@fpt.edu.vn"
    );

    // Đăng ký người dùng mới và gửi email xác thực
    public String registerUser(String email, String username, String password, User.Role selectRole) {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference users = firestore.collection(COLLECTION_NAME);


        // Kiểm tra email trong danh sách Admin
        User.Role role = ADMIN_Email.contains(email) ? User.Role.ADMIN : selectRole;

        System.out.println("Mật khẩu trước khi mã hoá: " + password);
        // ✅ Mã hóa mật khẩu trước khi lưu vào Firestore
        String hashedPassword = passService.encodePassword(password);
        System.out.println("Mật khẩu sau khi mã hoá: " + hashedPassword);
        User user = new User(username, email, username, hashedPassword, false, role); // Chưa xác thực tài khoản

        try {
            // ✅ Tạo OTP xác thực tài khoản
            String otp = emailService.generateOtp();
            long expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1); // Hết hạn sau 1 phút

            // ✅ Lưu OTP vào Firestore
            firestore.collection(ACCOUNT_OTP_COLLECTION).document(email).set(new OtpRecord(otp, expiryTime));

            // ✅ Gửi OTP qua email
            emailService.sendOtpEmailAccount(email, otp);
            System.out.println("📧 Đã gửi mã OTP xác thực tài khoản cho: " + email);
            System.out.println("Role của người dùng đăng ký: " + role);
            System.out.println("Dữ liệu User chuẩn bị lưu vào Firestore: " + user);

            // ✅ Lưu thông tin tài khoản vào Firestore (chưa xác thực)
            users.document(username).set(user);

            System.out.println("✅ Tài khoản đã được lưu vào Firestore (chưa xác thực): " + username);
            return "OTP đã được gửi đến email của bạn!";

        } catch (Exception e) {
            System.out.println("❌ Lỗi khi gửi OTP xác thực: " + e.getMessage());
            e.printStackTrace();
            return "Error: Không thể gửi OTP xác thực!";
        }
    }

    /**
     * Gửi OTP đặt lại mật khẩu và lưu vào Firestore
     */
    public String sendPasswordResetOtp(String email) {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference otpCollection = firestore.collection(OTP_COLLECTION);

        String otp = emailService.generateOtp();
        long expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1); // Hết hạn sau 1 phút

        try {
            otpCollection.document(email).set(new OtpRecord(otp, expiryTime));
            emailService.sendOtpEmail(email, otp);
            return "OTP đã được gửi đến email.";
        } catch (Exception e) {
            return "Lỗi khi gửi OTP: " + e.getMessage();
        }
    }

    /**
     * Gửi lại mã OTP khác
     */
    public String resendOtp(String email, String actionType) {
        Firestore firestore = FirestoreClient.getFirestore();
        String collectionName = actionType.equals("register") ? ACCOUNT_OTP_COLLECTION : OTP_COLLECTION;

        String newOtp = emailService.generateOtp();
        long expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);

        try {
            DocumentSnapshot existingOtp = firestore.collection(collectionName).document(email).get().get();

            if (existingOtp.exists()) {
                firestore.collection(collectionName).document(email).update("otp", newOtp, "expiryTime", expiryTime);
            } else {
                firestore.collection(collectionName).document(email).set(new OtpRecord(newOtp, expiryTime));
            }

            // ✅ Gửi đúng OTP theo loại yêu cầu
            if (actionType.equals("register")) {
                System.out.println("📧 Gửi lại OTP xác thực tài khoản đến: " + email);
                emailService.sendOtpEmailAccount(email, newOtp);
            } else {
                System.out.println("📧 Gửi lại OTP đặt lại mật khẩu đến: " + email);
                emailService.sendOtpEmail(email, newOtp);
            }

            return "Mã OTP mới đã được gửi!";
        } catch (Exception e) {
            return "Lỗi khi gửi lại OTP: " + e.getMessage();
        }
    }

    /**
     * Xác thực OTP trước khi cho phép đặt lại mật khẩu khi quên mật khẩu
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

    /**
     * Xác thực tài khoản bằng OTP
     */
    public boolean verifyAccountOtp(String email, String otp) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot otpSnapshot = firestore.collection(ACCOUNT_OTP_COLLECTION).document(email).get().get();
            if (!otpSnapshot.exists()) return false;

            OtpRecord otpRecord = otpSnapshot.toObject(OtpRecord.class);
            if (otpRecord == null || System.currentTimeMillis() > otpRecord.getExpiryTime()) return false;
            System.out.println("Đã qua đây");
            if (otpRecord.getOtp().equals(otp)) {
                // ✅ Cập nhật trạng thái xác thực tài khoản
                Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("email", email);
                QuerySnapshot querySnapshot = query.get().get();

                if (!querySnapshot.isEmpty()) {
                    DocumentSnapshot doc = querySnapshot.getDocuments().get(0);
                    doc.getReference().update("verified", true);
                    System.out.println("✅ Tài khoản đã được xác thực: " + email);
                }

                // ✅ Xóa OTP sau khi xác thực thành công
                firestore.collection(ACCOUNT_OTP_COLLECTION).document(email).delete();
                return true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    static class OtpRecord {
        private String otp;
        private long expiryTime;

        public OtpRecord() {
        }

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

    // Xác thực đăng nhập bằng username & password
    public boolean authenticate(String username, String password) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            // ✅ Lấy mật khẩu mã hóa từ Firestore
            String hashedPasswordFromDB = passService.getPasswordByUsername(username);
            if (hashedPasswordFromDB == null) {
                return false; // Không tìm thấy user
            }


            boolean match = passService.matches(password, hashedPasswordFromDB);
            return match;

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

    // ✅ Kiểm tra xem user có phải Admin không
    public boolean isAdmin(String username) {
        System.out.println("📌 Đang kiểm tra quyền admin của: " + username);  // ✅ Debug xem hàm có chạy không
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot userSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();
            if (!userSnapshot.exists()) {
                System.out.println("❌ Không tìm thấy user: " + username);
                return false;
            }

            String role = userSnapshot.getString("role");
            System.out.println("✅ Role của " + username + " là: " + role);
            return role != null && role.equalsIgnoreCase("ADMIN");

        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


}
