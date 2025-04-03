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
        String targetDetails = "";
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
        }
        targetDetails = (targetDetails != null && !targetDetails.isEmpty()) ? targetDetails : "Không tìm thấy";

        // Thay thế targetId trong description bằng targetDetails
        String description = log.getDescription();
        if (description != null && description.contains(log.getTargetId())) {
            description = description.replace(log.getTargetId(), targetDetails);
            log.setDescription(description);
        }

        // Lưu log vào Firestore
        Map<String, Object> logData = new HashMap<>();
        logData.put("adminUsername", log.getAdminUsername());
        logData.put("action", log.getAction());
        logData.put("targetType", log.getTargetType());
        logData.put("targetId", log.getTargetId());
        logData.put("targetName", log.getTargetName());
        logData.put("description", log.getDescription());
        logData.put("timestamp", Timestamp.now());
        logData.put("formattedTimestamp", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                .format(new java.util.Date()));

        ApiFuture<DocumentReference> future = db.collection("adminLogs").add(logData);
        future.get();
    }

    public Page<AdminLog> getAdminLogs(Pageable pageable, String searchQuery, String timeFilter)
            throws ExecutionException, InterruptedException {
        CollectionReference logsRef = db.collection("adminLogs");
        Query query = logsRef;

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

        // Phân trang ngay trong truy vấn Firestore
        query = query.offset((int) pageable.getOffset())
                .limit(pageable.getPageSize());

        ApiFuture<QuerySnapshot> querySnapshot = query.get();
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

            // Chuyển đổi Timestamp thành LocalDateTime
            Timestamp timestamp = doc.getTimestamp("timestamp");
            if (timestamp != null) {
                log.setTimestamp(timestamp.toDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDateTime());
            }

            // Lấy thông tin chi tiết của target (Post, Exam, hoặc User)
            String targetDetails = "";
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
            }
            log.setTargetDetails(targetDetails != null ? targetDetails : "Không tìm thấy");

            // Lọc tìm kiếm trong Java
            if (searchQuery != null && !searchQuery.trim().isEmpty()) {
                String searchLower = searchQuery.toLowerCase();
                if (!log.getAdminUsername().toLowerCase().contains(searchLower) &&
                        !log.getAction().toLowerCase().contains(searchLower)) {
                    continue;
                }
            }

            logs.add(log);
        }

        // Tính tổng số bản ghi
        ApiFuture<QuerySnapshot> countFuture = logsRef.get();
        long total = countFuture.get().size();

        return new PageImpl<>(logs, pageable, total);
    }
}