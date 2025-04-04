package com.example.exambuddy.service;

import com.example.exambuddy.model.AdminLog;
import com.google.api.core.ApiFuture;
import com.google.cloud.Timestamp;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class AdminLogService {
    private static final Firestore db = FirestoreClient.getFirestore();

    public void logAction(AdminLog log) throws ExecutionException, InterruptedException {
        if (log == null) {
            throw new IllegalArgumentException("AdminLog cannot be null");
        }
        if (log.getAdminUsername() == null || log.getAdminUsername().isEmpty()) {
            throw new IllegalArgumentException("Admin username cannot be null or empty");
        }
        if (log.getAction() == null || log.getAction().isEmpty()) {
            throw new IllegalArgumentException("Action cannot be null or empty");
        }

        // Lấy thông tin chi tiết của target (Post, Exam, hoặc User) để thay thế targetId trong description
        /*String targetDetails = "";
        if ("Post".equalsIgnoreCase(log.getTargetType())) {
            DocumentReference postRef = db.collection("posts").document(log.getTargetId());
            ApiFuture<DocumentSnapshot> postFuture = postRef.get();
            DocumentSnapshot postDoc = postFuture.get();
            if (postDoc.exists()) {
                targetDetails = postDoc.getString("content");
                if (targetDetails != null && targetDetails.length() > 50) {
                    targetDetails = targetDetails.substring(0, 47) + "...";
                }
            }
        } else if ("Exam".equalsIgnoreCase(log.getTargetType())) {
            DocumentReference examRef = db.collection("exams").document(log.getTargetId());
            ApiFuture<DocumentSnapshot> examFuture = examRef.get();
            DocumentSnapshot examDoc = examFuture.get();
            if (examDoc.exists()) {
                targetDetails = examDoc.getString("examName");
            }
        } else if ("User".equalsIgnoreCase(log.getTargetType())) {
            DocumentReference userRef = db.collection("users").document(log.getTargetId());
            ApiFuture<DocumentSnapshot> userFuture = userRef.get();
            DocumentSnapshot userDoc = userFuture.get();
            if (userDoc.exists()) {
                targetDetails = userDoc.getString("fullName");
                if (targetDetails == null || targetDetails.isEmpty()) {
                    targetDetails = userDoc.getString("username");
                }
            }
        }*/
        String targetDetails = "";
        if ("Post".equalsIgnoreCase(log.getTargetType())) {
            DocumentSnapshot postDoc = db.collection("posts").document(log.getTargetId()).get().get();
            if (postDoc.exists()) {
                targetDetails = postDoc.getString("content");
                if (targetDetails != null && targetDetails.length() > 50) {
                    targetDetails = targetDetails.substring(0, 47) + "...";
                }
            }
        } else if ("Exam".equalsIgnoreCase(log.getTargetType())) {
            DocumentSnapshot examDoc = db.collection("exams").document(log.getTargetId()).get().get();
            if (examDoc.exists()) {
                targetDetails = examDoc.getString("examName");
            }
        } else if ("User".equalsIgnoreCase(log.getTargetType())) {
            DocumentSnapshot userDoc = db.collection("users").document(log.getTargetId()).get().get();
            if (userDoc.exists()) {
                targetDetails = userDoc.getString("fullName");
                if (targetDetails == null || targetDetails.isEmpty()) {
                    targetDetails = userDoc.getString("username");
                }
            }
        }
        targetDetails = (targetDetails != null && !targetDetails.isEmpty()) ? targetDetails : "Không tìm thấy";

        // Thay thế targetId trong description bằng targetDetails
//        String description = log.getDescription();
//        if (description != null && description.contains(log.getTargetId())) {
//            description = description.replace(log.getTargetId(), targetDetails);
//            log.setDescription(description);
//        }
        String description = log.getDescription().replace(log.getTargetId(), targetDetails);

        // Lưu log vào Firestore
        Map<String, Object> logData = new HashMap<>();
        logData.put("adminUsername", log.getAdminUsername());
        logData.put("action", log.getAction());
        logData.put("targetType", log.getTargetType());
        logData.put("targetId", log.getTargetId());
        logData.put("targetName", log.getTargetName());
        logData.put("description", description);
        logData.put("targetDetails", targetDetails);
        logData.put("timestamp", Timestamp.now());

        db.collection("adminLogs").add(logData).get();
    }

    public Page<AdminLog> getAdminLogs(Pageable pageable, String searchQuery, String timeFilter)
            throws ExecutionException, InterruptedException {
        CollectionReference logsRef = db.collection("adminLogs");
        Query query = logsRef;

        // Áp dụng bộ lọc tìm kiếm trong Firestore
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String searchLower = searchQuery.toLowerCase();
            query = query.whereGreaterThanOrEqualTo("adminUsername", searchLower)
                    .whereLessThanOrEqualTo("adminUsername", searchLower + "\uf8ff");
        }

        // Áp dụng bộ lọc thời gian
        if (timeFilter != null && !timeFilter.isEmpty()) {
            Timestamp startTime;
            switch (timeFilter) {
                case "7days":
                    startTime = Timestamp.ofTimeSecondsAndNanos(
                            System.currentTimeMillis() / 1000 - 7 * 24 * 60 * 60, 0);
                    query = query.whereGreaterThanOrEqualTo("timestamp", startTime);
                    break;
                case "30days":
                    startTime = Timestamp.ofTimeSecondsAndNanos(
                            System.currentTimeMillis() / 1000 - 30 * 24 * 60 * 60, 0);
                    query = query.whereGreaterThanOrEqualTo("timestamp", startTime);
                    break;
                default:
                    break;
            }
        }

        // Sắp xếp
        if (pageable.getSort().isSorted()) {
            for (org.springframework.data.domain.Sort.Order order : pageable.getSort()) {
                query = query.orderBy(order.getProperty(),
                        order.getDirection().isAscending() ? Query.Direction.ASCENDING : Query.Direction.DESCENDING);
            }
        } else {
            query = query.orderBy("timestamp", Query.Direction.DESCENDING);
        }

        // Phân trang
        ApiFuture<QuerySnapshot> querySnapshot = query.offset((int) pageable.getOffset())
                .limit(pageable.getPageSize())
                .get();
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

        List<AdminLog> logs = new ArrayList<>();
        for (QueryDocumentSnapshot doc : documents) {
            AdminLog log = new AdminLog(
                    doc.getString("adminUsername"),
                    doc.getString("action"),
                    doc.getString("targetType"),
                    doc.getString("targetId"),
                    doc.getString("targetName"),
                    doc.getString("description")
            );
            log.setId(doc.getId());
            log.setTargetDetails(doc.getString("targetDetails")); // Lấy từ Firestore

            Timestamp timestamp = doc.getTimestamp("timestamp");
            if (timestamp != null) {
                log.setTimestamp(timestamp.toDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }

            logs.add(log);
        }

        // Đếm tổng số bản ghi (tạm giữ)
        long total = logsRef.get().get().size();

        return new PageImpl<>(logs, pageable, total);
    }
}