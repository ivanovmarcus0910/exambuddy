package com.example.exambuddy.service;

import com.example.exambuddy.model.RecordTopUser;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
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
    public List<RecordTopUser> getTopUsercContribute() {

        List<RecordTopUser> topUsers = new ArrayList<RecordTopUser>();

        try {
            ApiFuture<QuerySnapshot> future = db.collection("userContribute")
                    .orderBy("totalScore", Query.Direction.DESCENDING)
                    .limit(5)
                    .get();

            // Lấy kết quả từ truy vấn
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            // Lưu danh sách người dùng vào list
            for (QueryDocumentSnapshot doc : documents) {
                RecordTopUser record = doc.toObject(RecordTopUser.class);
                topUsers.add(record);
            }

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy top 5 người dùng: " + e.getMessage());
        }

        return topUsers;
    }


}
