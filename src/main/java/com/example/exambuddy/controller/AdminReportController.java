package com.example.exambuddy.controller;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.Feedback;
import com.example.exambuddy.model.ReportRequest;
import com.example.exambuddy.model.User;
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
import java.util.ArrayList;
import java.util.List;
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

    // Lấy danh sách báo cáo từ Firestore
    private List<ReportRequest> getReports() throws ExecutionException, InterruptedException {
        Firestore db = FirestoreClient.getFirestore();
        List<ReportRequest> reportList = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = db.collection("report").get();
        List<QueryDocumentSnapshot> documents = future.get().getDocuments();

        for (QueryDocumentSnapshot doc : documents) {
            ReportRequest report = doc.toObject(ReportRequest.class);
            report.setId(doc.getId()); // Giả sử ReportRequest có trường id để lưu document ID
            // Lấy thông tin bài đăng từ postService
            if (report.getPostId() != null) {
                var post = postService.getPostById(report.getPostId()); // Bạn cần viết hàm này nếu chưa có
                if (post != null) {
                    report.setPostContent(post.getContent()); // thêm getter/setter vào ReportRequest
                    report.setPostAuthor(post.getUsername());
                    report.setPostActive(post.isActive());
                }
            }

            reportList.add(report);
        }
        return reportList;
    }

    // Lấy danh sách tất cả feedback từ các đề thi
    private List<Feedback> getAllFeedbacks() throws ExecutionException, InterruptedException {
        List<Feedback> allFeedbacks = new ArrayList<>();
        List<Exam> exams = examService.getExamList(); // Lấy tất cả đề thi
        for (Exam exam : exams) {
            List<Feedback> feedbacks = feedbackService.getFeedbacksByExamId(exam.getExamID());
            allFeedbacks.addAll(feedbacks);
            // Kiểm tra và vô hiệu hóa đề thi nếu cần
            feedbackService.checkAndLockExam(exam.getExamID());
        }
        return allFeedbacks;
    }


    @GetMapping("")
    public String adminReports(Model model, HttpSession session) throws ExecutionException, InterruptedException {
        String loggedInUser = (String) session.getAttribute("loggedInUser");
        if (loggedInUser == null || !authService.isAdmin(loggedInUser)) {
            return "redirect:/login";
        }
        List<ReportRequest> reports = getReports();
        List<Feedback> feedbacks = getAllFeedbacks();
        model.addAttribute("reports", reports);
        model.addAttribute("feedbacks", feedbacks);
        User adminUser = userService.getUserByUsername(loggedInUser);
        model.addAttribute("adminUser", adminUser);
        return "adminReport"; // File Thymeleaf hiển thị báo cáo
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
