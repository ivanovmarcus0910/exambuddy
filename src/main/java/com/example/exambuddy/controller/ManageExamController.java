package com.example.exambuddy.controller;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.Question;
import com.example.exambuddy.service.CookieService;
import com.example.exambuddy.service.ExamService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class ManageExamController {

    private Firestore db = FirestoreClient.getFirestore();
    @Autowired
    private CookieService cookieService;
    @Autowired
    private ExamService examService;

    @GetMapping("/exams")
    public String listExams(Model model) {
        try {
            List<Exam> exams = examService.getExamList();
            model.addAttribute("exams", exams);
            return "examList"; // Trả về trang hiển thị danh sách đề thi
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải danh sách đề thi: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/exams/{examId}/detail")
    public String getExamDetail(@PathVariable String examId, Model model) {
        try {
            Exam exam = examService.getExam(examId);
            model.addAttribute("exam", exam);
            model.addAttribute("questions", exam.getQuestions());
            return "examDetail"; // Trả về trang HTML hiển thị đề thi
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải đề thi: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/exams/{examId}/do")
    public String doExam(@PathVariable String examId, HttpSession session, HttpServletRequest request, HttpServletResponse response, Model model) {
        try {
            if (session.getAttribute("loggedInUser") == null) {
                return "redirect:/home"; // Nếu chưa đăng nhập, chuyển hướng về home
            }

            Exam exam = examService.getExam(examId);
            model.addAttribute("exam", exam);
            model.addAttribute("questions", exam.getQuestions()); // Gửi danh sách câu hỏi qua view

            Map<String, Object> examSession = new HashMap<>();
            examSession.put("startTime", System.currentTimeMillis()); // Lưu thời gian hiện tại
            String username = cookieService.getCookie(request, "noname");
            examSession.put("username", username);
            examSession.put("examId", examId);
            db.collection("exam_sessions").document(username + "_" + examId).set(examSession);

            return "examDo";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi lấy đề thi: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("exams/{examId}/submit")
    public String submitExam(@PathVariable String examId, @RequestParam MultiValueMap<String, String> userAnswers, Model model) {

        userAnswers.forEach((questionID, answers) ->
                System.out.println("Câu " + questionID + " => " + answers)
        );

        // Chuyển đổi dữ liệu thành map đúng
        Map<String, List<Integer>> parsedAnswers = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : userAnswers.entrySet()) {
            List<Integer> selectedIndexes = entry.getValue().stream()
                    .map(Integer::parseInt) // Chuyển từ String sang Integer
                    .collect(Collectors.toList());
            parsedAnswers.put(entry.getKey(), selectedIndexes);
        }

        // In ra kết quả sau khi parse


        try {
            Exam exam = examService.getExam(examId);
            List<Question> questions = exam.getQuestions();
            int totalQuestions = exam.getQuestions().size();
            int correctCount = 0;
            Question question ;
            List<String> correctQuestions = new ArrayList<>();
            for (int i = 0; i < totalQuestions; i++) {
                String questionKey = "q" + i;
                question = questions.get(i);

                List<Integer> correctAnswers = question.getCorrectAnswers(); // Đáp án đúng
                List<Integer> userSelected = parsedAnswers.getOrDefault(questionKey, new ArrayList<>()); // Đáp án người dùng chọn

                // So sánh danh sách đáp án đúng với đáp án người dùng chọn
                if (new HashSet<>(correctAnswers).equals(new HashSet<>(userSelected))) {
                    correctCount++;
                    correctQuestions.add(questionKey); // Thêm vào danh sách câu đúng
                }
            }

            double score = (double) correctCount / totalQuestions * 10;

            model.addAttribute("exam", exam);
            model.addAttribute("score", score);
            model.addAttribute("totalQuestions", questions.size());
            model.addAttribute("userAnswers", userAnswers);
            model.addAttribute("correctQuestions", correctQuestions);
            return "examResult";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi nộp bài: " + e.getMessage());
            return "error";
        }
    }


    @GetMapping("/exams/addExam")
    public String addQuestionPage() {
        return "addExam.html";
    }

    @PostMapping("/exams/addExam")
    public String addQuestion(@RequestBody Map<String, Object> examData) {
        try {
            // Tạo ID ngẫu nhiên cho đề thi
            String examId = UUID.randomUUID().toString();

            // Thêm dữ liệu đề thi vào collection "exams"
            db.collection("exams").document(examId).set(Map.of(
                    "examName", examData.get("examName"),
                    "subject", examData.get("subject"),
                    "tags", examData.get("tags"),
                    "username", examData.get("username"),
                    "date", examData.get("date")
            )).get();

            // Thêm danh sách câu hỏi vào subcollection "questions"
            List<Map<String, Object>> questions = (List<Map<String, Object>>) examData.get("questions");
            WriteBatch batch = db.batch();
            for (Map<String, Object> question : questions) {
                String questionId = UUID.randomUUID().toString();
                batch.set(db.collection("exams").document(examId).collection("questions").document(questionId), question);
            }
            batch.commit().get();

            return "Đề thi đã được lưu thành công!";
        } catch (Exception e) {
            return "Lỗi khi lưu đề thi: " + e.getMessage();
        }
    }
}

