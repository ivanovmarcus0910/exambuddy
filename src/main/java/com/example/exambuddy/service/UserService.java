package com.example.exambuddy.service;

import com.example.exambuddy.model.Payment;
import com.example.exambuddy.model.User;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {
    private static final String COLLECTION_NAME = "users";

    public static User getUserData(String username) {
        Firestore firestore = FirestoreClient.getFirestore();

        try {
            DocumentSnapshot documentSnapshot = firestore.collection(COLLECTION_NAME)
                    .document(username)
                    .get()
                    .get();

            if (!documentSnapshot.exists()) {
                System.out.println("User not found in Firestore: " + username);
                return null;
            }

            User user = documentSnapshot.toObject(User.class);
            if (user == null) {
                System.out.println("Failed to map Firestore document to User object.");
                return null;
            }

            user.setUsername(username); // Đảm bảo username không bị null
            return user;

        } catch (Exception e) {
            System.out.println("Error fetching user data for: " + username);
            e.printStackTrace();
            return null;
        }
    }

    public static Boolean changeAvatar(String username, String url) {

        Firestore firestore = FirestoreClient.getFirestore();
        try {
            // 🔍 Tìm người dùng theo username trong Firestore
            DocumentSnapshot userSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();
            if (!userSnapshot.exists()) {
                System.out.println("❌ Không tìm thấy tài khoản với username: " + username);
                return false;
            }
            userSnapshot.getReference().update("avatarUrl", url);
        } catch (Exception e) {
            System.out.println("Lỗi khi đổi avatar");
            return false;
        }
        return true;
    }

    // ✅ Thêm mới hàm lưu tài khoản OAuth2 vào Firestore
    public static void saveOAuth2User(String email, String name, String avatarUrl) {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference users = firestore.collection(COLLECTION_NAME);

        try {
            // Kiểm tra xem email này đã có trong DB chưa
            User existingUser = getUserData(email);
            if (existingUser != null && existingUser.getEmail() != null) {
                System.out.println("Tài khoản đã tồn tại: " + email);
                return; // Nếu đã tồn tại thì bỏ qua
            }


            User user = new User();
            user.setEmail(email);
            user.setUsername(name);
            user.setAvatarUrl(avatarUrl);
            user.setPhone(""); // Để trống nếu không có sđt
            user.setPassword(""); // OAuth2 không có password
            user.setVerified(true); // Đăng nhập Google thì coi như đã xác thực luôn

            users.document(email).set(user);
            System.out.println("✅ Đã lưu tài khoản Google vào Firestore: " + email);

        } catch (Exception e) {
            System.out.println("❌ Lỗi khi lưu tài khoản Google vào Firestore: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String getAvatarUrlByUsername(String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            // Lấy dữ liệu của user từ Firestore
            DocumentSnapshot userSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();


            if (userSnapshot.exists()) {
                return userSnapshot.getString("avatarUrl"); // Lấy avatarUrl
            }
        } catch (Exception e) {
            System.out.println("Lỗi khi lấy avatar của: " + username);
        }
        return "http://res.cloudinary.com/dsuav027e/image/upload/v1739939318/dkm6iw7ujnsja8z9d3ek.png"; // Trả về avatar mặc định nếu không tìm thấy
    }

    public static void updateUserField(String username, String field, Object value) {
        Firestore firestore = FirestoreClient.getFirestore();
        DocumentReference userRef = firestore.collection(COLLECTION_NAME).document(username);

        Map<String, Object> updates = new HashMap<>();
        updates.put(field, value);

        try {
            userRef.update(updates).get();  // Cập nhật giá trị vào Firestore
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }


    public List<User> getAllUsers() throws ExecutionException, InterruptedException {
        Firestore firestore = FirestoreClient.getFirestore();
        List<User> userList = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = firestore.collection(COLLECTION_NAME).get();

        for (DocumentSnapshot document : future.get().getDocuments()) {
            User user = document.toObject(User.class);
            if (user != null) {
                user.setUsername(document.getId()); // Lấy ID từ Firestore
                userList.add(user);
            }
        }
        return userList;
    }

    public void updateUserRole(String username, User.Role role) {
        Firestore firestore = FirestoreClient.getFirestore();
        firestore.collection(COLLECTION_NAME).document(username).update("role", role.name());
    }

    public void deleteUser(String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        firestore.collection(COLLECTION_NAME).document(username).delete();
    }

    public User getUserByUsername(String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot userSnapshot = firestore.collection("users").document(username).get().get();
            return userSnapshot.exists() ? userSnapshot.toObject(User.class) : null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public boolean addPaymentTransaction(long paymentCode, int amoutn, String link, String status, String username) {
        Firestore firestore = FirestoreClient.getFirestore();
        CollectionReference transaction = firestore.collection("Transactions");
        Long time = System.currentTimeMillis();
        Payment payment = new Payment(paymentCode, amoutn, link, status, username, time);
        transaction.document().set(payment);
        return false;
    }

}
