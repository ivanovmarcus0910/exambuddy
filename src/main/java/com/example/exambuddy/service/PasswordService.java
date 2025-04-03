package com.example.exambuddy.service;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.firebase.auth.hash.Bcrypt;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class PasswordService {
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private static final String COLLECTION_NAME = "users";
    private static final String OTP_COLLECTION = "password_reset_otps"; // quen pass


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
                userRef.update("password", passwordEncoder.encode(newPassword));
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

    public boolean updatePasswordForUser(String username, String currentPassword, String newPassword) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            // 🔍 Tìm người dùng theo username trong Firestore
            DocumentSnapshot userSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();

            if (!userSnapshot.exists()) {
                System.out.println("❌ Không tìm thấy tài khoản với username: " + username);
                return false;
            }

            // ✅ Xác thực mật khẩu hiện tại trước khi cập nhật
            String storedPassword = userSnapshot.getString("password");
            if (!passwordEncoder.matches(currentPassword, storedPassword)) {
                System.out.println("❌ Mật khẩu hiện tại không đúng cho username: " + username);
                return false;
            }

            // ✅ Cập nhật mật khẩu mới
            userSnapshot.getReference().update("password", passwordEncoder.encode(newPassword));
            System.out.println("✅ Mật khẩu của " + username + " đã được cập nhật!");

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("❌ Lỗi khi cập nhật mật khẩu: " + e.getMessage());
            return false;
        }
    }

    // Mã hóa mật khẩu
    public String encodePassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // So sánh mật khẩu người dùng nhập với mật khẩu trong DB
    public boolean matches(String rawPassword, String encodedPassword) {
        System.out.println("🔍 So sánh mật khẩu:");
        System.out.println("   - Mật khẩu nhập vào: " + rawPassword);
        System.out.println("   - Mật khẩu từ DB: " + encodedPassword);
        boolean result = passwordEncoder.matches(rawPassword, encodedPassword);
        System.out.println("   - Kết quả: " + result);
        return result;
    }

    /**
     * Lấy mật khẩu mã hóa từ Firestore dựa trên username
     */
    public ApiFuture<String> getPasswordByUsername(String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        DocumentReference docRef = firestore.collection(COLLECTION_NAME).document(username);

        // Lấy ApiFuture<DocumentSnapshot> từ Firestore (không block)
        ApiFuture<DocumentSnapshot> futureSnapshot = docRef.get();

        // Chuyển đổi sang ApiFuture<String> với hàm transform
        return ApiFutures.transform(
                futureSnapshot,
                (DocumentSnapshot userSnapshot) -> {
                    if (!userSnapshot.exists()) {
                        System.out.println("❌ Không tìm thấy user với username: " + username);
                        return null;
                    }
                    return userSnapshot.getString("password");
                },
                MoreExecutors.directExecutor()
        );
    }

    // Phương thức kiểm tra mật khẩu với điều kiện mới
    public boolean isValidPassword(String password) {
        if (password == null) {
            return false;
        }
        // Regex: ít nhất 8 ký tự, có chữ hoa, chữ thường, số và ký tự đặc biệt
        return password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");
    }
}
