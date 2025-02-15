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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
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
            String username = cookieService.getCookie(request, "noname");
            examService.addExamSession(examId, username, 1000*60*30);
            model.addAttribute("exam", exam);
            model.addAttribute("username", username);
            model.addAttribute("questions", exam.getQuestions()); // Gửi danh sách câu hỏi qua view
            model.addAttribute("timeduration", 1000*60*30);


            return "examDo";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi lấy đề thi: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("exams/{examId}/submit")
    public String submitExam(@PathVariable String examId, @RequestParam MultiValueMap<String, String> userAnswers, HttpServletRequest request ,Model model) {

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
                    //System.out.println("Đúng "+ i);
                    correctCount++;
                    correctQuestions.add(questionKey); // Thêm vào danh sách câu đúng
                }
                //System.out.println(i+":"+correctAnswers +" vs "+ userSelected);
            }

            double score = (double) correctCount / totalQuestions * 10;
            userAnswers.forEach((questionID, answers) ->
                    System.out.println(questionID + " => " + answers)
            );
            String username = cookieService.getCookie(request, "noname");
            examService.submitExam(username, examId);
            examService.saveExamResult(examId, score, parsedAnswers);
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
            boolean status = examService.addExam(examData);
            if (status)
                return "Đề thi đã được lưu thành công!";
            else throw new Exception();
        } catch (Exception e) {
            return "Lỗi khi lưu đề thi: " + e.getMessage();
        }
    }

    @GetMapping("exams/progress")
    public ResponseEntity<?> getProgress(@RequestParam String userId, @RequestParam String examId) {
        Map<String, Object> progress = examService.getProgress(userId, examId);
        return ResponseEntity.ok(progress);
    }

    @PostMapping("exams/progress")
    public ResponseEntity<?> saveProgress(@RequestBody Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        String examId = (String) payload.get("examId");
        Map<String, Object> answers = (Map<String, Object>) payload.get("answers");

        examService.saveProgress(userId, examId, answers);
        return ResponseEntity.ok(Collections.singletonMap("status", "success"));
    }
    @GetMapping("exams/time-left")
    public ResponseEntity<?> getTimeLeft(@RequestParam String userId, @RequestParam String examId) {
        System.out.println(" Ở controller :userId = " + userId + " examId = " + examId);
        long timeLeft = examService.getRemainingTime(userId, examId);
        boolean submitted = examService.isSubmitted(userId, examId);
        System.out.println("Time left in controller : " + timeLeft);
        return ResponseEntity.ok(Map.of("timeLeft", timeLeft, "submitted", submitted));
    }
}

