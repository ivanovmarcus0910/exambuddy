package com.example.exambuddy.service;

import com.example.exambuddy.model.AdminLog;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class AdminLogService {
    private static final Firestore db = FirestoreClient.getFirestore();

    public void logAction(AdminLog log) {
        Map<String, Object> logData = new HashMap<>();
        logData.put("adminUsername", log.getAdminUsername());
        logData.put("action", log.getAction());
        logData.put("targetType", log.getTargetType());
        logData.put("targetId", log.getTargetId());
        logData.put("description", log.getDescription());
        logData.put("timestamp", com.google.cloud.Timestamp.now());
        ApiFuture<?> future = db.collection("adminLogs").add(logData);
        try {
            System.out.println("Log được ghi: " + future.get());
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Lỗi khi ghi log: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
