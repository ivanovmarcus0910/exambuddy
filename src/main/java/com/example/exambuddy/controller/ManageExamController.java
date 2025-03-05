package com.example.exambuddy.controller;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.ExamResult;
import com.example.exambuddy.model.Question;
import com.example.exambuddy.model.User;
import com.example.exambuddy.service.CookieService;
import com.example.exambuddy.service.ExamService;
import com.example.exambuddy.service.UserService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

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
    @Autowired
    private UserService userService;

    @GetMapping("/exams")
    public String listExams(Model model) {
        try {
            List<Exam> exams = examService.getExamList(0, 6, 6);
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
            if (exam == null) {
                model.addAttribute("error", "Không tìm thấy đề thi với ID: " + examId);
                return "error";
            }
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
            examService.addExamSession(examId, username, 1000 * 60 * 30);
            model.addAttribute("exam", exam);
            model.addAttribute("username", username);
            model.addAttribute("questions", exam.getQuestions()); // Gửi danh sách câu hỏi qua view
            model.addAttribute("timeduration", 60 * 30);
            return "examDo";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi lấy đề thi: " + e.getMessage());
            return "error";
        }
    }

    @PostMapping("exams/{examId}/submit")
    public String submitExam(@PathVariable String examId, @RequestParam MultiValueMap<String, String> userAnswers, HttpServletRequest request, Model model) {

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
            Question question;
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
    public String addExam(@RequestBody Map<String, Object> examData, HttpSession session, HttpServletRequest request) {
        // Lấy username từ session
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về trang đăng nhập
        }

        // Lấy thông tin user từ UserService
        User user = userService.getUserByUsername(username);
        if (user == null || (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.TEACHER)) {
            return "error"; // Chuyển hướng hoặc hiển thị lỗi nếu không có quyền
        }

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
        long x = System.currentTimeMillis();
        long timeLeft = examService.getRemainingTime(userId, examId);
        boolean submitted = examService.isSubmitted(userId, examId);
        return ResponseEntity.ok(Map.of("timeLeft", timeLeft / 1000, "submitted", submitted));
    }

    @GetMapping("exams/result-list")
    public String getResultList(HttpServletRequest request, HttpSession session, Model model) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/home"; // Nếu chưa đăng nhập, chuyển hướng về home
        }
        String username = cookieService.getCookie(request, "noname");
        User user =userService.getUserByUsername(username);
        model.addAttribute("user", user);
        List<ExamResult> results = username != null ? examService.getExamResultByUsername(username) : new ArrayList<>();
        model.addAttribute("results", results);
        model.addAttribute("username", username);
        return "examResultList";
    }


    @PostMapping("/exams/{id}/like")
    @ResponseBody
    public ResponseEntity<?> likeExam(@PathVariable String id, HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Bạn cần đăng nhập để like."));
        }
        String username = cookieService.getCookie(request, "noname");
        examService.likeExam(username, id);

        return ResponseEntity.ok(Map.of("message", "Bạn đã like bài thi!"));
    }

    @DeleteMapping("/exams/{id}/like")
    @ResponseBody
    public ResponseEntity<?> unlikeExam(@PathVariable String id, HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Bạn cần đăng nhập để unlike."));
        }

        String username = cookieService.getCookie(request, "noname");
        examService.unlikeExam(username, id);

        return ResponseEntity.ok(Map.of("message", "Bạn đã bỏ like bài thi!"));
    }

    @GetMapping("/exams/{id}/isLiked")
    @ResponseBody
    public ResponseEntity<?> isLiked(@PathVariable String id, HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.ok(Map.of("liked", false)); // Nếu chưa đăng nhập, coi như chưa like
        }

        String username = cookieService.getCookie(request, "noname");
        boolean isLiked = examService.isExamLiked(username, id);

        return ResponseEntity.ok(Map.of("liked", isLiked));
    }

    @GetMapping("/exams/liked")
    public String showLikedExams(Model model, HttpServletRequest request, HttpSession session) {
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
        User user = userService.getUserByUsername(username);
        model.addAttribute("user", user);
        List<Exam> createdExams = examService.getHtoryCreateExamsByUsername(username);
        model.addAttribute("createdExams", createdExams);
        return "createdExams"; // Tên file HTML để hiển thị các bài thi đã tạo
    }


    @GetMapping("/exams/result/{resultId}/{examId}")
    public String viewExamResult(@PathVariable String resultId, @PathVariable String examId, Model model, HttpServletRequest request, HttpSession session) throws ExecutionException, InterruptedException {
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
        } else {
            examList = examService.getExamList(0, 10, 0);
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
        grade = (grade != null) ? grade : "";
        subject = (subject != null) ? subject : "";
        examType = (examType != null) ? examType : "";
        city = (city != null) ? city : "";
        List<Exam> examList = examService.searchExamsByFilter(grade, subject, examType, city);
        model.addAttribute("examList", examList);
        return "resultSearchExam";
    }

    @PostMapping("/exams/importExcel")
    public ResponseEntity<?> importExamFromExcel(
            @RequestParam("file") MultipartFile file,
            @RequestParam("examName") String examName,
            @RequestParam("grade") String grade,
            @RequestParam("subject") String subject,
            @RequestParam("examType") String examType,
            @RequestParam("city") String city,
            @RequestParam("tags") String tags,
            HttpSession session,
            HttpServletRequest request) {
        // Check if user is logged in
        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Bạn cần đăng nhập để import."));
        }

        // Get username from cookie
        String username = cookieService.getCookie(request, "noname");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Không tìm thấy thông tin người dùng."));
        }

        // Prepare exam data
        Map<String, Object> examData = new HashMap<>();
        examData.put("examName", examName);
        examData.put("grade", grade);
        examData.put("subject", subject);
        examData.put("examType", examType);
        examData.put("city", city);
        examData.put("tags", Arrays.asList(tags.split(","))); // Convert tags to List
        examData.put("username", username);

        try {
            // Call the service method to import from Excel
            boolean success = examService.importExamFromExcel(file.getInputStream(), examData);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Đã import đề thi từ file Excel thành công!"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Lỗi khi import file Excel."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi xử lý file: " + e.getMessage()));
        }
    }
    @PostMapping("/exams/importDocx")
    public ResponseEntity<?> importExamFromDocx(
            @RequestParam("file") MultipartFile file,
            @RequestParam("examName") String examName,
            @RequestParam("grade") String grade,
            @RequestParam("subject") String subject,
            @RequestParam("examType") String examType,
            @RequestParam("city") String city,
            @RequestParam("tags") String tags,
            HttpSession session,
            HttpServletRequest request) {
        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Bạn cần đăng nhập để import."));
        }

        String username = cookieService.getCookie(request, "noname");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Không tìm thấy thông tin người dùng."));
        }

        Map<String, Object> examData = new HashMap<>();
        examData.put("examName", examName);
        examData.put("grade", grade);
        examData.put("subject", subject);
        examData.put("examType", examType);
        examData.put("city", city);
        examData.put("tags", Arrays.asList(tags.split(","))); // Chuyển String[] thành List<String>
        examData.put("username", username);

        try {
            boolean success = examService.importExamFromDocx(file.getInputStream(), examData);
            if (success) {
                return ResponseEntity.ok(Map.of("message", "Đã import đề thi từ file DOCX thành công!"));
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body(Map.of("message", "Lỗi khi import file DOCX."));
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi khi xử lý file: " + e.getMessage()));
        }
    }
}

