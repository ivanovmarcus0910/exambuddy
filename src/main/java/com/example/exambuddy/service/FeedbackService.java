package com.example.exambuddy.service;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.Feedback;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FeedbackService {
    @Autowired
    private ExamService examService;

    private static final Logger log = LoggerFactory.getLogger(FeedbackService.class);
    private static final Firestore db = FirestoreClient.getFirestore();
    private static final String COLLECTION_NAME = "exams";

    public boolean hasUserCommented(String examId, String username) {
        if (username == null || examId == null) {
            return false; // Nếu username hoặc examId null, coi như chưa comment
        }
        List<Feedback> feedbacks = getFeedbacksByExamId(examId);
        return feedbacks.stream()
                .anyMatch(f -> username.equals(f.getUsername()) && !f.isReply());
    }

    public Feedback saveFeedback(String examId, String username, String avatarUrl, String content,
                                 String date, int rate) {
        if (examId == null || username == null || content == null || content.trim().isEmpty()) {
            log.warn("Dữ liệu đầu vào không hợp lệ khi lưu feedback cho examId: {}", examId);
            return null;
        }
        if (hasUserCommented(examId, username)) {
            log.warn("Người dùng {} đã bình luận cho examId: {}", username, examId);
            return null;
        }
        DocumentReference examRef = db.collection(COLLECTION_NAME).document(examId);
        CollectionReference feedbackRef = examRef.collection("feedback");

        Feedback feedback = new Feedback();
        feedback.setExamId(examId);
        feedback.setUsername(username);
        feedback.setAvatarUrl(avatarUrl != null ? avatarUrl : "");
        feedback.setContent(content.trim());
        feedback.setDate(date);
        feedback.setRate(rate);
        feedback.setReply(false);

        try {
            DocumentReference newFeedbackRef = feedbackRef.add(feedback).get();
            feedback.setFeedbackId(newFeedbackRef.getId());
            log.info("Feedback lưu thành công với ID: {}", feedback.getFeedbackId());

            checkAndLockExam(examId);

            return feedback;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Lỗi khi lưu feedback: {}", e.getMessage(), e);
            return null;
        }
    }

    // Thêm phương thức kiểm tra và vô hiệu hóa đề thi
    public void checkAndLockExam(String examId) {
        List<Feedback> feedbacks = getFeedbacksByExamId(examId);
        long lowRatingCount = feedbacks.stream()
                .filter(f -> !f.isReply() && (f.getRate() == 1 || f.getRate() == 2))
                .count();

        if (lowRatingCount >= 2) { // Ngưỡng 5 như yêu cầu ban đầu
            examService.disableExam(examId);
            log.info("Đề thi {} đã bị vô hiệu hóa tự động do có {} đánh giá 1-2 sao.", examId, lowRatingCount);
        }
    }

    public Map<String, Object> getRatingSummary(String examId) {
        List<Feedback> feedbacks = getFeedbacksByExamId(examId);
        Map<Integer, Integer> starCount = new HashMap<>();
        int totalRate = 0;
        int count = 0;

        // Khởi tạo 1-5 sao
        for (int i = 1; i <= 5; i++) {
            starCount.put(i, 0);
        }

        for (Feedback f : feedbacks) {
            if (!f.isReply()) {
                int rate = f.getRate();
                starCount.put(rate, starCount.getOrDefault(rate, 0) + 1);
                totalRate += rate;
                count++;
            }
        }

        double average = count > 0 ? (double) totalRate / count : 0;

        Map<String, Object> result = new HashMap<>();
        result.put("average", average);
        result.put("total", count);
        result.put("starCount", starCount);
        return result;
    }

    public Feedback saveReply(String examId, String parentFeedbackId, String username,String avatarUrl, String content, String date) {
        if (examId == null || parentFeedbackId == null || username == null || content == null || content.trim().isEmpty()) {
            log.warn("Dữ liệu đầu vào không hợp lệ khi lưu reply cho examId: {}", examId);
            return null;
        }
        DocumentReference examRef = db.collection(COLLECTION_NAME).document(examId);
        CollectionReference feedbackRef = examRef.collection("feedback");

        Feedback reply = new Feedback();
        reply.setExamId(examId);
        reply.setUsername(username);
        reply.setAvatarUrl(avatarUrl != null ? avatarUrl : "");
        reply.setContent(content.trim());
        reply.setDate(date);
        reply.setParentFeedbackId(parentFeedbackId);
        reply.setReply(true);

        try {
            DocumentReference newFeedbackRef = feedbackRef.add(reply).get();
            reply.setFeedbackId(newFeedbackRef.getId());
            log.info("Reply lưu thành công với ID: {}", reply.getFeedbackId());
            return reply;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Lỗi khi lưu reply: {}", e.getMessage(), e);
            return null;
        }
    }

    public List<Feedback> getFeedbacksByExamId(String examId) {
        List<Feedback> feedbacks = new ArrayList<>();
        if (examId == null) {
            log.warn("examId không hợp lệ khi lấy feedback");
            return feedbacks;
        }
        try {
            DocumentReference examRef = db.collection(COLLECTION_NAME).document(examId);
            CollectionReference feedbackRef = examRef.collection("feedback");
            ApiFuture<QuerySnapshot> future = feedbackRef.get();
            QuerySnapshot querySnapshot = future.get();

            Exam exam = examService.getExam(examId);
            String examTitle = (exam != null) ? exam.getExamName() : "Đề thi không tồn tại";
            for (QueryDocumentSnapshot document : querySnapshot.getDocuments()) {
                Feedback feedback = document.toObject(Feedback.class);
                feedback.setFeedbackId(document.getId());
                feedback.setExamTitle(examTitle);
                feedbacks.add(feedback);
            }
            log.info("Lấy được {} feedback/reply cho examId: {}", feedbacks.size(), examId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Lỗi khi lấy feedback: {}", e.getMessage(), e);
        }
        return feedbacks;
    }

    public Feedback updateFeedback(String examId, Feedback feedback) {
        if (examId == null || feedback == null || feedback.getFeedbackId() == null) {
            log.warn("Dữ liệu không hợp lệ khi cập nhật feedback");
            return null;
        }
        DocumentReference feedbackRef = db.collection(COLLECTION_NAME)
                .document(examId)
                .collection("feedback")
                .document(feedback.getFeedbackId());

        try {
            ApiFuture<WriteResult> writeResult = feedbackRef.set(feedback);
            writeResult.get();
            log.info("Feedback cập nhật thành công: {}", feedback.getFeedbackId());
            return feedback;
        } catch (InterruptedException | ExecutionException e) {
            log.error("Lỗi khi cập nhật feedback: {}", e.getMessage(), e);
            return null;
        }
    }

    public void deleteFeedback(String examId, String feedbackId) {
        if (examId == null || feedbackId == null) {
            log.warn("Dữ liệu không hợp lệ khi xóa feedback");
            return;
        }
        DocumentReference feedbackRef = db.collection(COLLECTION_NAME)
                .document(examId)
                .collection("feedback")
                .document(feedbackId);

        try {
            ApiFuture<WriteResult> writeResult = feedbackRef.delete();
            writeResult.get();
            log.info("Feedback xóa thành công: {}", feedbackId);
        } catch (InterruptedException | ExecutionException e) {
            log.error("Lỗi khi xóa feedback: {}", e.getMessage(), e);
            throw new RuntimeException("Lỗi khi xóa feedback: " + e.getMessage());
        }
    }
}