package com.example.exambuddy.service;

import com.example.exambuddy.model.RecordTopUser;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import com.google.api.core.ApiFuture;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

@Service
public class LeaderBoardService {
    private final Firestore db = FirestoreClient.getFirestore();
    @Async
    public CompletableFuture<List<RecordTopUser>> getTopUserScore() {
        List<RecordTopUser> topUsers = new ArrayList<>();
        try {
            ApiFuture<QuerySnapshot> future = db.collection("userScores")
                    .orderBy("totalScore", Query.Direction.DESCENDING)
                    .limit(5)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (QueryDocumentSnapshot doc : documents) {
                RecordTopUser record = doc.toObject(RecordTopUser.class);
                topUsers.add(record);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy top 5 người dùng: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(topUsers);
    }
    @Async
    public CompletableFuture<List<RecordTopUser>> getTopUserContribute() {
        List<RecordTopUser> topUsers = new ArrayList<>();
        try {
            ApiFuture<QuerySnapshot> future = db.collection("userContribute")
                    .orderBy("totalScore", Query.Direction.DESCENDING)
                    .limit(5)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (QueryDocumentSnapshot doc : documents) {
                RecordTopUser record = doc.toObject(RecordTopUser.class);
                topUsers.add(record);
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi lấy top 5 người dùng: " + e.getMessage());
        }
        return CompletableFuture.completedFuture(topUsers);
    }
    public void updateUserContribute(String username, double newScore) {
        DocumentReference userScoreRef = db.collection("userContribute").document(username);

        try {
            // Kiểm tra xem document có tồn tại không
            if (userScoreRef.get().get().exists()) {
                // Nếu tồn tại, cập nhật điểm số
                userScoreRef.update("totalScore", FieldValue.increment(newScore)).get();
                System.out.println("Cập nhật điểm thành công!");
            } else {
                // Nếu chưa có, tạo mới document
                Map<String, Object> data = new HashMap<>();
                data.put("username", username);
                data.put("totalScore", newScore);
                userScoreRef.set(data).get();
                System.out.println("Tạo mới và cập nhật điểm thành công!");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật điểm: " + e.getMessage());
        }
    }

    public void updateUserScore(String username, double newScore) {
        DocumentReference userScoreRef = db.collection("userScores").document(username);

        try {
            // Kiểm tra xem document có tồn tại không
            if (userScoreRef.get().get().exists()) {
                // Nếu tồn tại, cập nhật điểm số
                userScoreRef.update("totalScore", FieldValue.increment(newScore)).get();
                System.out.println("Cập nhật điểm thành công!");
            } else {
                // Nếu chưa có, tạo mới document
                Map<String, Object> data = new HashMap<>();
                data.put("username", username);
                data.put("totalScore", newScore);
                userScoreRef.set(data).get();
                System.out.println("Tạo mới và cập nhật điểm thành công!");
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi cập nhật điểm: " + e.getMessage());
        }
    }

}
