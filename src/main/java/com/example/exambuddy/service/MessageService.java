package com.example.exambuddy.service;

import com.example.exambuddy.model.ReportRequest;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.FieldValue;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class MessageService {

    private static final Firestore db = FirestoreClient.getFirestore();

    // Autowire PostService để có thể gọi hàm cập nhật trạng thái bài đăng
    @Autowired
    private PostService postService;

    public void saveReport(ReportRequest request) {
        System.out.println("🔍 request.getReasons() = " + request.getReason());
        Map<String, Object> reportData = new HashMap<>();
        reportData.put("postId", request.getPostId());
        reportData.put("reason", request.getReason());
        reportData.put("description", request.getDescription());
        reportData.put("reporter", request.getReporter()); // Người báo cáo
        reportData.put("timestamp", FieldValue.serverTimestamp());


        // Gửi dữ liệu lên Firestore
        ApiFuture<DocumentReference> future = db.collection("report").add(reportData);
        try {
            DocumentReference document = future.get(); // Chờ đến khi Firestore lưu xong
            System.out.println("Báo cáo đã được lưu với ID: " + document.getId());

            // Sau khi lưu báo cáo, kiểm tra số lượng báo cáo cho bài đăng này
            Query query = db.collection("report").whereEqualTo("postId", request.getPostId());
            ApiFuture<QuerySnapshot> querySnapshotFuture = query.get();
            int reportCount = querySnapshotFuture.get().size(); // Số lượng báo cáo

            System.out.println("Số lượng báo cáo của bài đăng " + request.getPostId() + " là: " + reportCount);

            // Nếu số lượng báo cáo >= 3, tự động vô hiệu hóa bài đăng
            if (reportCount >= 3) {
                postService.updatePostStatus(request.getPostId(), false);
                System.out.println("Bài đăng " + request.getPostId() + " đã được vô hiệu hóa do quá nhiều báo cáo.");
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Lỗi khi lưu báo cáo: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
