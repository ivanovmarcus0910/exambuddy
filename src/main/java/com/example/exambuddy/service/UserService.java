package com.example.exambuddy.service;

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
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class UserService {
    private static final String COLLECTION_NAME = "users";
    public static User getUserData(String username){
        Firestore firestore = FirestoreClient.getFirestore();
        User user = new User();
        try{
            DocumentSnapshot documentSnapshot = firestore.collection(COLLECTION_NAME).document(username).get().get();
            if (!documentSnapshot.exists()) return user;
            user.setEmail(documentSnapshot.getString("email"));
            user.setPhone(documentSnapshot.getString("phone"));
            user.setUsername(documentSnapshot.getString("username"));
            user.setAvatarUrl(documentSnapshot.getString("avatarUrl"));
        }
        catch (Exception e){

        }

        return user;
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
        }
            catch (Exception e){
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

    public User getUserByEmail(String email) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            DocumentSnapshot document = firestore.collection(COLLECTION_NAME).document(email).get().get();
            if (document.exists()) {
                return document.toObject(User.class);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    //Hàm đổi role của user
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

}
