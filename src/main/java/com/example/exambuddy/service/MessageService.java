package com.example.exambuddy.service;

import com.example.exambuddy.model.ReportRequest;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class MessageService {
    private static final Firestore db = FirestoreClient.getFirestore();

    public void saveReport(ReportRequest request) {
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("postId", request.getPostId());
        reportData.put("reason", request.getReasons());
        reportData.put("description", request.getDescription());
        reportData.put("reporter", request.getReporter()); // Người báo cáo
        reportData.put("timestamp", FieldValue.serverTimestamp());

        // Gửi dữ liệu lên Firestore
        ApiFuture<DocumentReference> future = db.collection("report").add(reportData);
        try {
            DocumentReference document = future.get(); // Chờ đến khi Firestore lưu xong
            System.out.println("Báo cáo đã được lưu với ID: " + document.getId());
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Lỗi khi lưu báo cáo: " + e.getMessage());
        }
    }
}
