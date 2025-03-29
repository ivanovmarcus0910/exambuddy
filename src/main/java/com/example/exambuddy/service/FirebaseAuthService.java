package com.example.exambuddy.service;

import com.example.exambuddy.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
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

        // Nếu vai trò là Teacher, lưu vào cơ sở dữ liệu với vai trò Pending_Teacher
            if (role == User.Role.TEACHER) {
                role = User.Role.PENDING_TEACHER;  // Chuyển thành Pending_Teacher
            }

        System.out.println("Mật khẩu trước khi mã hoá: " + password);
        // ✅ Mã hóa mật khẩu trước khi lưu vào Firestore
        String hashedPassword = passService.encodePassword(password);
        System.out.println("Mật khẩu sau khi mã hoá: " + hashedPassword);
        User user = new User(username, email, username, hashedPassword, false, role); // Chưa xác thực tài khoản


        try {
            // ✅ Tạo OTP xác thực tài khoản
            String otp = emailService.generateOtp();
            long expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1); // Hết hạn sau 1 phút

            // Lưu OTP vào Firestore (khởi tạo resendCount = 0, lockTime = 0)
            OtpRecord otpRecord = new OtpRecord(otp, expiryTime);
            FirestoreClient.getFirestore().collection(ACCOUNT_OTP_COLLECTION).document(email).set(otpRecord);

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
            OtpRecord otpRecord = new OtpRecord(otp, expiryTime);
            otpCollection.document(email).set(otpRecord);
            emailService.sendOtpEmail(email, otp);
            return "OTP đã được gửi đến email.";
        } catch (Exception e) {
            return "Lỗi khi gửi OTP: " + e.getMessage();
        }
    }

    /**
     * Gửi lại mã OTP khác
     */
    /*public CompletableFuture<String> resendOtp(String email, String actionType) {
        Firestore firestore = FirestoreClient.getFirestore();
        String collectionName = actionType.equals("register") ? ACCOUNT_OTP_COLLECTION : OTP_COLLECTION;
        String newOtp = emailService.generateOtp();
        long expiryTime = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(1);

        DocumentReference docRef = firestore.collection(collectionName).document(email);

        ApiFuture<DocumentSnapshot> snapshotApiFuture = docRef.get();

        return toCompletableFuture(snapshotApiFuture)
                .thenCompose(existingOtp -> {
                    CompletableFuture<WriteResult> updateFuture;
                    if (existingOtp.exists()) {
                        ApiFuture<WriteResult> updateApiFuture = docRef.update("otp", newOtp, "expiryTime", expiryTime);
                        updateFuture = toCompletableFuture(updateApiFuture);
                    } else {
                        ApiFuture<WriteResult> setApiFuture = docRef.set(new OtpRecord(newOtp, expiryTime));
                        updateFuture = toCompletableFuture(setApiFuture);
                    }
                    return updateFuture.thenApply(writeResult -> {
                        if (actionType.equals("register")) {
                            System.out.println("📧 Gửi lại OTP xác thực tài khoản đến: " + email);
                            try {
                                emailService.sendOtpEmailAccount(email, newOtp);
                            } catch (MessagingException e) {
                                throw new RuntimeException(e);
                            }
                        } else {
                            System.out.println("📧 Gửi lại OTP đặt lại mật khẩu đến: " + email);
                            try {
                                emailService.sendOtpEmail(email, newOtp);
                            } catch (MessagingException e) {
                                throw new RuntimeException(e);
                            }
                        }
                        return "Mã OTP mới đã được gửi!";
                    });
                })
                .exceptionally(ex -> "Lỗi khi gửi lại OTP: " + ex.getMessage());
    }*/
    public CompletableFuture<String> resendOtp(String email, String actionType) {
        Firestore firestore = FirestoreClient.getFirestore();
        String collectionName = actionType.equals("register") ? ACCOUNT_OTP_COLLECTION : OTP_COLLECTION;
        DocumentReference docRef = firestore.collection(collectionName).document(email);
        long now = System.currentTimeMillis();

        return CompletableFuture.supplyAsync(() -> {
            try {
                // Sử dụng transaction để đảm bảo tính nguyên tử
                String result = firestore.runTransaction(transaction -> {
                    DocumentSnapshot snapshot = transaction.get(docRef).get();
                    OtpRecord record;
                    if (snapshot.exists()) {
                        record = snapshot.toObject(OtpRecord.class);
                        if (record == null) {
                            record = new OtpRecord();
                        }
                    } else {
                        record = new OtpRecord();
                    }

                    // Kiểm tra resendCount và lockTime
                    if (record.getResendCount() >= 1) {
                        if (now - record.getLockTime() < TimeUnit.MINUTES.toMillis(2)) {
                            // Nếu chưa hết thời gian khóa, trả về thông báo lỗi
                            return "Bạn đã gửi OTP quá nhiều lần. Vui lòng thử lại sau 1 phút.";
                        } else {
                            // Sau 1 phút, reset lại số lần gửi và lockTime
                            record.setResendCount(0);
                            record.setLockTime(0);
                        }
                    }

                    // Tạo OTP mới và cập nhật thời gian hết hạn
                    String newOtp = emailService.generateOtp();
                    long expiryTime = now + TimeUnit.MINUTES.toMillis(1);
                    record.setOtp(newOtp);
                    record.setExpiryTime(expiryTime);

                    // Cập nhật số lần gửi
                    int newCount = record.getResendCount() + 1;
                    record.setResendCount(newCount);
                    // Nếu số lần gửi đạt giới hạn, đặt lockTime
                    if (newCount >= 1) {
                        record.setLockTime(now);
                    }

                    // Lưu lại record đã cập nhật trong transaction
                    transaction.set(docRef, record);

                    // Gửi OTP qua email
                    if (actionType.equals("register")) {
                        System.out.println("📧 Gửi lại OTP xác thực tài khoản đến: " + email);
                        emailService.sendOtpEmailAccount(email, newOtp);
                    } else {
                        System.out.println("📧 Gửi lại OTP đặt lại mật khẩu đến: " + email);
                        emailService.sendOtpEmail(email, newOtp);
                    }
                    return "Mã OTP mới đã được gửi!";
                }).get();
                return result;
            } catch (Exception e) {
                return "Lỗi khi gửi lại OTP: " + e.getMessage();
            }
        });
    }

    /**
     * Chuyển đổi ApiFuture<T> thành CompletableFuture<T>
     */
    private <T> CompletableFuture<T> toCompletableFuture(ApiFuture<T> apiFuture) {
        CompletableFuture<T> completableFuture = new CompletableFuture<>();
        apiFuture.addListener(() -> {
            try {
                completableFuture.complete(apiFuture.get());
            } catch (Exception e) {
                completableFuture.completeExceptionally(e);
            }
        }, Executors.newSingleThreadExecutor());
        return completableFuture;
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

    public static class OtpRecord {
        private String otp;
        private long expiryTime;
        private int resendCount;
        private long lockTime;

        public OtpRecord() {
            this.resendCount = 0;
            this.lockTime = 0;
        }

        public OtpRecord(String otp, long expiryTime) {
            this.otp = otp;
            this.expiryTime = expiryTime;
            this.resendCount = 0;
            this.lockTime = 0;
        }

        public String getOtp() {
            return otp;
        }

        public void setOtp(String otp) {
            this.otp = otp;
        }

        public long getExpiryTime() {
            return expiryTime;
        }

        public void setExpiryTime(long expiryTime) {
            this.expiryTime = expiryTime;
        }

        public int getResendCount() {
            return resendCount;
        }

        public void setResendCount(int resendCount) {
            this.resendCount = resendCount;
        }

        public long getLockTime() {
            return lockTime;
        }

        public void setLockTime(long lockTime) {
            this.lockTime = lockTime;
        }
    }

    //Kiểm tra xem email đã xác thực chưa
    public boolean isEmailVerified(String email) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            // 🔍 Truy vấn Firestore để tìm user theo email
            Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("email", email);
            QuerySnapshot querySnapshot = query.get().get();

            // Kiểm tra xem có user nào với email này không
            if (querySnapshot.isEmpty()) {
                System.out.println("❌ Không tìm thấy user nào với email: " + email);
                return false;
            }

            // Lấy document đầu tiên (nếu có nhiều kết quả, Firestore lấy kết quả đầu tiên)
            DocumentSnapshot userSnapshot = querySnapshot.getDocuments().get(0);
            Boolean verified = userSnapshot.getBoolean("verified");

            // In ra để debug
            System.out.println("🔍 Giá trị verified từ Firestore cho " + email + ": " + verified);

            // Xử lý nếu trường "verified" bị null (chưa tồn tại)
            if (verified == null) {
                System.out.println("⚠️ Trường verified không tồn tại hoặc bị null!");
                return false;
            }

            return verified;  // Trả về true nếu đã xác thực
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    // Xác thực đăng nhập bằng username & password
    public CompletableFuture<Boolean> authenticate(String username, String password) {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        Firestore firestore = FirestoreClient.getFirestore();

        // Giả sử passService.getPasswordByUsernameAsync trả về ApiFuture<String>
        ApiFuture<String> hashedPasswordFuture = passService.getPasswordByUsername(username);

        hashedPasswordFuture.addListener(() -> {
            try {
                String hashedPasswordFromDB = hashedPasswordFuture.get();
                if (hashedPasswordFromDB == null) {
                    future.complete(false);
                } else {
                    boolean match = passService.matches(password, hashedPasswordFromDB);
                    future.complete(match);
                }
            } catch (Exception e) {
                e.printStackTrace();
                future.complete(false);
            }
        }, Executors.newSingleThreadExecutor());

        return future;
    }


    // Kiểm tra xem email đã tồn tại chưa
    public CompletableFuture<Boolean> isEmailExists(String email) {
        Firestore firestore = FirestoreClient.getFirestore();
        Query query = firestore.collection(COLLECTION_NAME).whereEqualTo("email", email);
        ApiFuture<QuerySnapshot> queryFuture = query.get();

        return CompletableFuture.supplyAsync(() -> {
            try {
                QuerySnapshot snapshot = queryFuture.get();
                return !snapshot.isEmpty();
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        });
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

    // Phương thức cập nhật số lần đăng nhập thất bại
    public void updateFailedLogin(HttpSession session) {
        Integer failedAttempts = (Integer) session.getAttribute("failedLoginCount");
        if (failedAttempts == null) {
            failedAttempts = 1;
        } else {
            failedAttempts++;
        }
        session.setAttribute("failedLoginCount", failedAttempts);
        // Nếu đã nhập sai >= 5 lần, đặt thời gian lock hiện tại
        if (failedAttempts >= 5) {
            session.setAttribute("loginLockTime", System.currentTimeMillis());
        }
    }

}
