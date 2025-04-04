package com.example.exambuddy.controller;

import com.example.exambuddy.model.*;
import com.example.exambuddy.service.*;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import jakarta.servlet.http.HttpSession;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/admin/reports")
public class AdminReportController {

    @Autowired
    private FirebaseAuthService authService;

    @Autowired
    private UserService userService;

    @Autowired
    private PostService postService;

    @Autowired
    private FeedbackService feedbackService;

    @Autowired
    private ExamService examService;

    private CompletableFuture<List<ReportRequest>> getReports() {
        Firestore db = FirestoreClient.getFirestore();
        ApiFuture<QuerySnapshot> future = db.collection("report").get();

        return CompletableFuture.supplyAsync(() -> {
            try {
                List<QueryDocumentSnapshot> documents = future.get().getDocuments();
                List<String> postIds = new ArrayList<>();
                Map<String, ReportRequest> reportMap = new HashMap<>();

                // Bước 1: Thu thập ID bài đăng và dữ liệu báo cáo ban đầu
                for (QueryDocumentSnapshot doc : documents) {
                    ReportRequest report = doc.toObject(ReportRequest.class);
                    report.setId(doc.getId());
                    reportMap.put(report.getId(), report);
                    if (report.getPostId() != null) {
                        postIds.add(report.getPostId());
                    }
                }

                // Bước 2: Lấy tất cả bài đăng trong một truy vấn
                if (!postIds.isEmpty()) {
                    ApiFuture<QuerySnapshot> postFuture = db.collection("posts")
                            .whereIn("postId", postIds)
                            .get();
                    List<QueryDocumentSnapshot> postDocs = postFuture.get().getDocuments();
                    for (QueryDocumentSnapshot postDoc : postDocs) {
                        Post post = postDoc.toObject(Post.class);
                        reportMap.values().stream()
                                .filter(r -> r.getPostId().equals(postDoc.getId()))
                                .forEach(r -> {
                                    r.setPostContent(post.getContent());
                                    r.setPostAuthor(post.getUsername());
                                    r.setPostActive(post.isActive());
                                });
                    }
                }
                return new ArrayList<>(reportMap.values());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
                return new ArrayList<>();
            }
        });
    }

    // Lấy danh sách tất cả feedback từ các đề thi
    private CompletableFuture<List<Feedback>> getAllFeedbacks() {
        List<Feedback> allFeedbacks = Collections.synchronizedList(new ArrayList<>());
        return CompletableFuture.supplyAsync(() -> {
            try {
                List<Exam> exams = examService.getExamList(); // Giả sử hàm này đã đồng bộ, nếu không thì cần tối ưu thêm
                List<CompletableFuture<Void>> futures = new ArrayList<>();

                for (Exam exam : exams) {
                    CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                        try {
                            List<Feedback> feedbacks = feedbackService.getFeedbacksByExamId(exam.getExamID());
                            allFeedbacks.addAll(feedbacks);
                            feedbackService.checkAndLockExam(exam.getExamID());
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });
                    futures.add(future);
                }

                // Đợi tất cả các tác vụ hoàn thành
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
                return allFeedbacks;
            } catch (Exception e) {
                e.printStackTrace();
                return allFeedbacks;
            }
        });
    }


    @GetMapping("")
    public CompletableFuture<String> adminReports(Model model, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return CompletableFuture.completedFuture("redirect:/login");
        }

        CompletableFuture<List<ReportRequest>> reportsFuture = getReports();
        CompletableFuture<List<Feedback>> feedbacksFuture = getAllFeedbacks();

        return CompletableFuture.allOf(reportsFuture, feedbacksFuture)
                .thenApply(v -> {
                    try {
                        model.addAttribute("reports", reportsFuture.get());
                        model.addAttribute("feedbacks", feedbacksFuture.get());
                        User adminUser = userService.getUserByUsername(loggedInUser);
                        model.addAttribute("adminUser", adminUser);
                        return "adminReport";
                    } catch (Exception e) {
                        e.printStackTrace();
                        return "redirect:/login";
                    }
                });
    }

    // Action khoá bài đăng từ báo cáo
    @PostMapping("/lockPost")
    public String lockPost(@RequestParam String postId, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }
        postService.updatePostStatus(postId, false);
        return "redirect:/admin/reports";
    }

    // Action khóa đề thi từ feedback
    @PostMapping("/lockExam")
    public String lockExam(@RequestParam String examId, HttpSession session) {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }
        examService.disableExam(examId);
        return "redirect:/admin/reports";
    }

    // Action xoá báo cáo
    @PostMapping("/delete")
    public String deleteReport(@RequestParam String reportId, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }
        Firestore db = FirestoreClient.getFirestore();
        db.collection("report").document(reportId).delete().get();
        return "redirect:/admin/reports";
    }

    // Action xóa feedback
    @PostMapping("/deleteFeedback")
    public String deleteFeedback(@RequestParam String examId, @RequestParam String feedbackId, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }
        feedbackService.deleteFeedback(examId, feedbackId);
        feedbackService.checkAndLockExam(examId); // Kiểm tra lại sau khi xóa feedback
        return "redirect:/admin/reports";
    }
}
