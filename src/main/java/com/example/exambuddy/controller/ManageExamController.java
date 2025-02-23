package com.example.exambuddy.controller;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.ExamResult;
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
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutionException;

@Slf4j
@Controller
public class ManageExamController {
    private Firestore db = FirestoreClient.getFirestore();
    public ManageExamController(ExamService examService) {
        this.examService = examService;
    }
    @Autowired
    private CookieService cookieService;
    @Autowired
    private ExamService examService;

    @GetMapping("/exams")
    public String listExams(Model model) {
        try {
            List<Exam> exams = examService.getExamList(0,6);
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
            model.addAttribute("timeduration", 60*30);
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
            examService.saveExamResult(username, examId, score, exam, userAnswers, correctQuestions);
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
        System.out.println("Time left in controller : " + timeLeft/1000);
        return ResponseEntity.ok(Map.of("timeLeft", timeLeft/1000, "submitted", submitted));
    }
    @GetMapping("exams/result-list")
    public String getResultList(HttpServletRequest request, HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/home"; // Nếu chưa đăng nhập, chuyển hướng về home
        }
        String username = cookieService.getCookie(request, "noname");
        List<ExamResult> results = username != null ? examService.getExamResultByUsername(username) : new ArrayList<>();
        model.addAttribute("results", results);
        model.addAttribute("username", username);
        return "examResultList";
    }


    @GetMapping("/exams/{id}/like")
    public String likeExam(@PathVariable String id,HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về home
        }
        String username = cookieService.getCookie(request, "noname");
        examService.likeExam(username, id);
        return "redirect:/home"; // Sau khi like, quay lại danh sách bài thi
    }

    @GetMapping("/exams/liked")
    public String showLikedExams(Model model,HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về home
        }
        String username = cookieService.getCookie(request, "noname");
        List<Exam> likedExams = examService.getLikedExamsByUser(username);

        model.addAttribute("likedExams", likedExams);
        return "likedExams"; // Trả về giao diện liked-exams.html
    }


    @GetMapping("/exams/created")
    public String showCreatedExams(Model model, HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về home
        }
        String username = cookieService.getCookie(request, "noname");
        List<Exam> createdExams = examService.getHtoryCreateExamsByUsername(username);
        model.addAttribute("createdExams", createdExams);
        return "createdExams"; // Tên file HTML để hiển thị các bài thi đã tạo
    }



    @GetMapping("/exams/result/{resultId}/{examId}")
    public String viewExamResult(@PathVariable String resultId,@PathVariable String examId, Model model,HttpServletRequest request, HttpSession session) throws ExecutionException, InterruptedException {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về home
        }
        String username = cookieService.getCookie(request, "noname");

        // Lấy kết quả bài thi của user
        ExamResult examResult = examService.getExamResult(resultId);
        if (examResult == null) {
            return "redirect:/home";
        }

        // Lấy danh sách câu hỏi từ Firestore
        Exam exam = examService.getExam(examId);
        if (exam == null) {
            return "redirect:/exams/result-list";
        }

        model.addAttribute("exam", exam);  // Truyền danh sách câu hỏi vào Thymeleaf
        model.addAttribute("score", examResult.getScore());
        model.addAttribute("userAnswers", examResult.getAnswers());
        model.addAttribute("correctQuestions", examResult.getCorrectAnswers());

        return "examResultDetail";
    }
    @GetMapping("/search")
    public String viewExamSearch(@RequestParam(required = false) String examName, Model model)
            throws ExecutionException, InterruptedException {
        List<Exam> examList;
        if (examName != null && !examName.isEmpty()) {
            examList = examService.searchExamByName(examName);
        }else{
            examList = examService.getExamList(0,10);
        }
        model.addAttribute("examList", examList);
        return "resultSearchExam";
    }
    @GetMapping("/search-by-filter")
    public String searchExams(@RequestParam(required = false) String grade,
                              @RequestParam(required = false) String subject,
                              @RequestParam(required = false) String examType,
                              @RequestParam(required = false) String city,
                              Model model) {
        // Xử lý giá trị mặc định cho các tham số
        grade = (grade != null) ? grade : "";
        subject = (subject != null) ? subject : "";
        examType = (examType != null) ? examType : "";
        city = (city != null) ? city : "";

        List<Exam> examList = examService.searchExamsByFilter(grade, subject, examType, city);
        model.addAttribute("examList", examList);
        return "resultSearchExam";
    }


}

