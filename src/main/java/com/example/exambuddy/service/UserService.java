package com.example.exambuddy.service;

import com.example.exambuddy.model.User;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.servlet.http.HttpSession;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

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



}
