package com.example.exambuddy.controller;

import com.example.exambuddy.model.*;
import com.example.exambuddy.service.CookieService;
import com.example.exambuddy.service.ExamService;
import com.example.exambuddy.service.LeaderBoardService;
import com.example.exambuddy.service.UserService;
import com.google.api.core.ApiFuture;
import com.example.exambuddy.service.*;
import com.example.exambuddy.model.User;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import java.util.concurrent.ExecutionException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


@Slf4j
@Controller
public class ManageExamController {
    @Autowired
    private LeaderBoardService leaderBoardService;

    public ManageExamController(ExamService examService) {
        this.examService = examService;
    }

    @Autowired
    private CookieService cookieService;
    @Autowired
    private ExamService examService;
    @Autowired
    private UserService userService;
    @Autowired
    private FeedbackService feedbackService;
//    @GetMapping("/exams")
//    public String listExams(Model model) {
//        try {
//            List<Exam> exams = examService.getExamList(0, 6);
//            model.addAttribute("exams", exams);
//            return "examList"; // Trả về trang hiển thị danh sách đề thi
//        } catch (Exception e) {
//            model.addAttribute("error", "Lỗi khi tải danh sách đề thi: " + e.getMessage());
//            return "error";
//        }
//    }

    @GetMapping("/exams/{examId}/detail")
    public String getExamDetail(@PathVariable String examId,Model model, HttpServletRequest request, HttpSession session) {

        try {
            Exam exam = examService.getExam(examId);
            if (exam == null) {
                model.addAttribute("error", "Không tìm thấy đề thi với ID: " + examId);
                return "error";
            }
            String username = (String) session.getAttribute("loggedInUser");
            if (username == null) {
                username = cookieService.getCookie(request, "noname");
            }
            boolean isLoggedIn = username != null && !username.trim().isEmpty();
            boolean hasCommented = isLoggedIn && feedbackService.hasUserCommented(examId, username);
            boolean isExamCreator = isLoggedIn && userService.isExamCreator(examId, username);
            List<Feedback> feedbacks = feedbackService.getFeedbacksByExamId(examId);

            model.addAttribute("exam", exam);
            model.addAttribute("questions", exam.getQuestions());
            model.addAttribute("feedbacks", feedbacks);
            model.addAttribute("username", username);
            model.addAttribute("isLoggedIn", isLoggedIn);
            model.addAttribute("hasCommented", hasCommented);
            model.addAttribute("isExamCreator", isExamCreator);
            Map<String, Object> ratingSummary = feedbackService.getRatingSummary(examId);
            model.addAttribute("ratingSummary", ratingSummary);
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
                return "redirect:/home";
            }
            String username = session.getAttribute("loggedInUser").toString();
            User user = userService.getUserData(username);

            Exam exam = examService.getExam(examId);
            if (userService.limitAccess(user))
            {
                boolean isLoggedIn = username != null && !username.trim().isEmpty();
                boolean hasCommented = isLoggedIn && feedbackService.hasUserCommented(examId, username);
                boolean isExamCreator = isLoggedIn && userService.isExamCreator(examId, username);
                List<Feedback> feedbacks = feedbackService.getFeedbacksByExamId(examId);
                model.addAttribute("err", "Bạn đạt giới hạn làm bài miễn phí trong ngày, vui lòng nâng cấp lên Premium để không bị giới hạn");

                model.addAttribute("exam", exam);
                model.addAttribute("questions", exam.getQuestions());
                model.addAttribute("feedbacks", feedbacks);
                model.addAttribute("username", username);
                model.addAttribute("isLoggedIn", isLoggedIn);
                model.addAttribute("hasCommented", hasCommented);
                model.addAttribute("isExamCreator", isExamCreator);
                Map<String, Object> ratingSummary = feedbackService.getRatingSummary(examId);
                model.addAttribute("ratingSummary", ratingSummary);
                return "examDetail"; // Trả về trang HTML hiển thị đề thi
            }
            List<Question> questions;
            if (exam.isFromQuestionBank()) {
                questions = examService.generateQuestionsFromPool(exam.getQuestionPool(), exam.getChapterConfig());
            } else {
                questions = new ArrayList<>(exam.getQuestions());
            }

            if (session.getAttribute("shuffledQuestions_" + examId) == null) {
                // Gộp cả logic từ bạn A và bạn
                examService.deleteProcess(username, examId); // Xoá tiến trình cũ
                examService.addExamSession(examId, username, 1000L * 60 * exam.getTimeduration());
                Collections.shuffle(questions);
                session.setAttribute("shuffledQuestions_" + examId, questions);
            } else {
                questions = (List<Question>) session.getAttribute("shuffledQuestions_" + examId);
            }

            model.addAttribute("exam", exam);
            model.addAttribute("username", username);
            model.addAttribute("questions", questions);
            model.addAttribute("timeduration", exam.getTimeduration());
            return "examDo";
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi lấy đề thi: " + e.getMessage());
            return "error";
        }
    }


    @PostMapping("/exams/create-from-bank")
    @ResponseBody
    public ResponseEntity<?> createExamFromBank(
            @RequestBody Map<String, Object> request,
            HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập");
        }
        try {
            @SuppressWarnings("unchecked")
            Map<String, Integer> chapterConfig = (Map<String, Integer>) request.get("chapterConfig");


            int timeduration = Integer.parseInt(request.get("timeduration").toString());


            Exam exam = examService.createExamFromBank(
                    (String) request.get("examName"),
                    (String) request.get("grade"),
                    (String) request.get("subject"),
                    (String) request.get("examType"),
                    (String) request.get("city"),
                    timeduration,
                    chapterConfig,
                    username
            );


            return ResponseEntity.ok(Map.of(
                    "examId", exam.getExamID(),
                    "message", "Tạo đề thành công"
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @GetMapping("/api/exams/chapters")
    @ResponseBody
    public ResponseEntity<Map<String, Integer>> getAvailableChapters(
            @RequestParam String subject,
            @RequestParam String grade,
            HttpSession session) {


        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }


        try {
            Map<String, Integer> chapters = examService.getAvailableChapters(username, subject, grade);
            return ResponseEntity.ok(chapters);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }


    @PostMapping("exams/{examId}/submit")
    public String submitExam(@PathVariable String examId, @RequestParam MultiValueMap<String, String> userAnswers, HttpServletRequest request, Model model, HttpSession session) {

        userAnswers.forEach((questionID, answers) ->
                System.out.println("Câu " + questionID + " => " + answers)
        );
        Map<String, List<Integer>> parsedAnswers = new HashMap<>();
        for (Map.Entry<String, List<String>> entry : userAnswers.entrySet()) {
            List<Integer> selectedIndexes = entry.getValue().stream()
                    .map(Integer::parseInt) // Chuyển từ String sang Integer
                    .collect(Collectors.toList());
            parsedAnswers.put(entry.getKey(), selectedIndexes);
        }


        try {
            Exam exam = examService.getExam(examId);
            List<Question> questions = (List<Question>) session.getAttribute("shuffledQuestions_" + examId);
            exam.setQuestions(questions);
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
//                System.out.println(i+":"+correctAnswers +" vs "+ userSelected);
            }

            double score = (double) correctCount / totalQuestions * 10;
//            userAnswers.forEach((questionID, answers) ->
//                    System.out.println(questionID + " => " + answers)
//            );
            String username = session.getAttribute("loggedInUser").toString();
            User user = userService.getUserByUsername(username);
            String avatarUrl = user != null ? user.getAvatarUrl() : "";
            session.removeAttribute("shuffledQuestions_"+examId);
            examService.submitExam(username, examId);
            examService.saveExamResult(username, examId, score, exam, userAnswers, correctQuestions);
            leaderBoardService.updateUserScore(username, score, avatarUrl);
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
    @ResponseBody // Đảm bảo trả về JSON, không render template
    public ResponseEntity<Map<String, Object>> addExam(@RequestBody Map<String, Object> examData, HttpSession session, HttpServletRequest request) {
        Map<String, Object> response = new HashMap<>();


        // Lấy username từ session
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            response.put("status", "error");
            response.put("message", "redirect:/login");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }


        // Lấy thông tin user từ UserService
        User user = userService.getUserByUsername(username);
        if (user == null || (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.TEACHER)) {
            response.put("status", "error");
            response.put("message", "Bạn không có quyền tạo đề thi!");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
        }


        try {
            boolean status = examService.addExam(examData, "");
            if (status) {
                response.put("status", "success");
                response.put("message", "Đề thi đã được lưu thành công!");
                return ResponseEntity.ok(response);
            } else {
                response.put("status", "error");
                response.put("message", "Lỗi khi lưu đề thi!");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
        } catch (Exception e) {
            response.put("status", "error");
            response.put("message", "Lỗi khi lưu đề thi: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }



    //Lấy tiến trình bài đang làm
    @GetMapping("exams/progress")
    public ResponseEntity<?> getProgress(@RequestParam String userId, @RequestParam String examId) {
        Map<String, Object> progress = examService.getProgress(userId, examId);
        return ResponseEntity.ok(progress);
    }
    //Lưu tiến trình bài đang làm
    @PostMapping("exams/progress")
    public ResponseEntity<?> saveProgress(@RequestBody Map<String, Object> payload) {
        String userId = (String) payload.get("userId");
        String examId = (String) payload.get("examId");
        Map<String, Object> answers = (Map<String, Object>) payload.get("answers");

        examService.saveProgress(userId, examId, answers);
        return ResponseEntity.ok(Collections.singletonMap("status", "success"));
    }
    //Lấy thời gian còn lại
    @GetMapping("exams/time-left")
    public ResponseEntity<?> getTimeLeft(@RequestParam String userId, @RequestParam String examId) {
        long x = System.currentTimeMillis();
        long timeLeft = examService.getRemainingTime(userId, examId);
        boolean submitted = examService.isSubmitted(userId, examId);
        return ResponseEntity.ok(Map.of("timeLeft", timeLeft / 1000, "submitted", submitted));
    }
    //Lấy danh sách lịch sử làm bài
    @GetMapping("exams/result-list")
    public String getResultList(HttpServletRequest request, HttpSession session, Model model,
                                @RequestParam(defaultValue = "0") int page, // Thêm tham số page
                                @RequestParam(defaultValue = "10") int size) { // Thêm tham số size
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/home"; // Nếu chưa đăng nhập, chuyển hướng về home
        }

        String username = cookieService.getCookie(request, "noname");
        User user = userService.getUserByUsername(username);
        model.addAttribute("user", user);
        // Tạo đối tượng Pageable để truyền vào ExamService
        Pageable pageable = PageRequest.of(page, size);

        // Gọi ExamService để lấy danh sách phân trang
        Page<ExamResult> resultPage = username != null ? examService.getExamResultByUsername(username, pageable) : Page.empty();

        // Thêm các thuộc tính vào model để giao diện sử dụng
        model.addAttribute("results", resultPage.getContent()); // Danh sách kết quả trong trang hiện tại
        model.addAttribute("currentPage", resultPage.getNumber()); // Trang hiện tại
        model.addAttribute("totalPages", resultPage.getTotalPages()); // Tổng số trang
        model.addAttribute("totalItems", resultPage.getTotalElements()); // Tổng số kết quả
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
    public CompletableFuture<ResponseEntity<Map<String, Boolean>>> isLiked(
            @PathVariable String id, HttpServletRequest request, HttpSession session) {

        if (session.getAttribute("loggedInUser") == null) {
            return CompletableFuture.completedFuture(ResponseEntity.ok(Map.of("liked", false)));
        }

        String username = cookieService.getCookie(request, "noname");

        return examService.isExamLikedAsync(username, id)
                .thenApplyAsync(isLiked -> ResponseEntity.ok(Map.of("liked", isLiked)));
    }


    @GetMapping("/exams/liked")
    public ResponseEntity<List<Map<String, Object>>> showLikedExams(
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "searchQuery", required = false) String searchQuery,
            HttpServletRequest request, HttpSession session) {

        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String username = cookieService.getCookie(request, "noname");
        List<Exam> likedExams = examService.getLikedExamsByUser(username);

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            likedExams = likedExams.stream()
                    .filter(exam -> exam.getExamName() != null &&
                            exam.getExamName().toLowerCase().contains(searchQuery.toLowerCase()))
                    .collect(Collectors.toList());
        }

        likedExams = examService.filterExamsBySubject(likedExams, subject);
        likedExams = examService.filterExamsByClass(likedExams, grade);

        // Chuyển đổi danh sách Exam thành JSON đơn giản
        List<Map<String, Object>> response = likedExams.stream().map(exam -> {
            Map<String, Object> map = new HashMap<>();
            map.put("examName", exam.getExamName());
            map.put("subject", exam.getSubject());
            map.put("grade", exam.getGrade());
            map.put("examID", exam.getExamID());
            map.put("createdDate", exam.getFormattedDate());
            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    //Created List
    @GetMapping("/exams/created")
    public String showCreatedExams(
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "grade", required = false) String grade,
            @RequestParam(value = "status", required = false) String status, // Thêm tham số status
            @RequestParam(value = "searchQuery", required = false) String searchQuery,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            Model model, HttpServletRequest request, HttpSession session) {

        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về login
        }

        String username = cookieService.getCookie(request, "noname");
        User user = userService.getUserByUsername(username);
        model.addAttribute("user", user);

        // Tạo đối tượng Pageable để truyền vào service
        Pageable pageable = PageRequest.of(page, size, Sort.by("date").descending());

        // Lấy danh sách bài kiểm tra đã tạo bởi username với phân trang
        Page<Exam> examPage = examService.getHistoryCreateExamsByUsername(username, pageable);

        // Lấy danh sách exams từ Page
        List<Exam> createdExams = examPage.getContent();
        System.out.println("createdExams: " + createdExams);

        // Áp dụng tìm kiếm theo tên
        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            createdExams = createdExams.stream()
                    .filter(exam -> exam.getExamName() != null && exam.getExamName().toLowerCase().contains(searchQuery.toLowerCase()))
                    .collect(Collectors.toList());
        }

        // Áp dụng bộ lọc theo môn học, grade và trạng thái
        createdExams = examService.filterExamsBySubject(createdExams, subject);
        createdExams = examService.filterExamsByClass(createdExams, grade);
        createdExams = examService.filterExamsByStatus(createdExams, status); // Thêm bộ lọc theo trạng thái

        // Truyền dữ liệu sang view
        model.addAttribute("createdExams", createdExams);
        model.addAttribute("currentPage", examPage.getNumber());
        model.addAttribute("totalPages", examPage.getTotalPages());
        model.addAttribute("totalItems", examPage.getTotalElements());
        model.addAttribute("pageSize", size);
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("selectedGrade", grade);
        model.addAttribute("selectedStatus", status); // Thêm selectedStatus vào model
        model.addAttribute("searchQuery", searchQuery);

        return "createdExams";
    }


    @GetMapping("/exams/result/{resultId}/{examId}")
    public String viewExamResult(@PathVariable String resultId, @PathVariable String examId, Model model, HttpServletRequest request, HttpSession session) {
        try {
            // Kiểm tra đăng nhập
            if (session.getAttribute("loggedInUser") == null) {
                return "redirect:/login";
            }
            String username = cookieService.getCookie(request, "noname");
            if (username == null) {
                model.addAttribute("err", "Không tìm thấy thông tin người dùng.");
                model.addAttribute("exam", new Exam());
                model.addAttribute("score", 0);
                model.addAttribute("userName", "Không xác định");
                model.addAttribute("userAnswers", null);
                model.addAttribute("correctQuestions", null);
                return "fragments/ResultDetail :: detailFragment";
            }

            // Lấy kết quả bài thi
            ExamResult examResult = examService.getExamResult(resultId);
            if (examResult == null) {
                model.addAttribute("err", "Không tìm thấy kết quả bài thi.");
                model.addAttribute("exam", new Exam());
                model.addAttribute("score", 0);
                model.addAttribute("userName", username);
                model.addAttribute("userAnswers", null);
                model.addAttribute("correctQuestions", null);
                return "fragments/ResultDetail :: detailFragment";
            }

            // Lấy thông tin đề thi hiện tại từ examService
            Exam exam = examService.getExam(examId);

            // Kiểm tra xem exam bị vô hiệu hóa hay không
            if (exam == null || "DISABLED".equals(exam.getStatus())) {
                model.addAttribute("exam", new Exam()); // Đối tượng rỗng để tránh lỗi template
                model.addAttribute("score", examResult.getScore());
                model.addAttribute("userName", examResult.getUsername() != null ? examResult.getUsername() : username);
                model.addAttribute("userAnswers", null);
                model.addAttribute("correctQuestions", examResult.getCorrectAnswers());
                model.addAttribute("err", "Lỗi đề thi đã bị vô hiệu hóa, chân thành xin lỗi!");
                return "fragments/ResultDetail :: detailFragment";
            }

            // Nếu đề thi không bị vô hiệu hóa, hiển thị chi tiết
            Map<String, List<String>> userAnswers = new HashMap<>();
            if (exam.getQuestions() != null) {
                for (int i = 0; i < exam.getQuestions().size(); i++) {
                    String key = "q" + i;
                    userAnswers.put(key, examResult.getAnswers() != null ?
                            examResult.getAnswers().getOrDefault(key, Collections.emptyList()) : Collections.emptyList());
                }
            }

            model.addAttribute("exam", exam);
            model.addAttribute("score", examResult.getScore());
            model.addAttribute("userAnswers", userAnswers);
            model.addAttribute("correctQuestions", examResult.getCorrectAnswers());
            model.addAttribute("userName", examResult.getUsername() != null ? examResult.getUsername() : username);

            return "fragments/ResultDetail :: detailFragment";
        } catch (Exception e) {
            log.error("Lỗi khi xem kết quả bài thi: resultId={}, examId={}, error={}", resultId, examId, e.getMessage(), e);
            model.addAttribute("exam", new Exam());
            model.addAttribute("score", 0);
            model.addAttribute("userName", "Không xác định");
            model.addAttribute("userAnswers", null);
            model.addAttribute("correctQuestions", null);
            model.addAttribute("err", "Lỗi đề thi đã bị vô hiệu hóa, chân thành xin lỗi!");
            return "fragments/ResultDetail :: detailFragment";
        }
    }

    @GetMapping("/search")
    public String viewExamSearch(@RequestParam(required = false) String examName, Model model)
            throws ExecutionException, InterruptedException {
        List<Exam> examList;
        if (examName != null && !examName.isEmpty()) {
            examList = examService.searchExamByName(examName);
        } else {
            examList = examService.getExamList();
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
            HttpSession session) {


        if (session.getAttribute("loggedInUser") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("message", "Bạn cần đăng nhập để import."));
        }


        try {
            List<Map<String, Object>> questions = examService.extractQuestionsFromDocx(file.getInputStream());
            return ResponseEntity.ok(Map.of("questions", questions, "message", "Import thành công!"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("message", "Lỗi xử lý file DOCX: " + e.getMessage()));
        }
    }




    @GetMapping("/participant")
    public String participant() {
        return "examStatistics";}

    @GetMapping("/exams/{examId}/statistics")
    public String getExamStatistics(@PathVariable String examId, Model model,
                                    HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login";
        }

        String username = session.getAttribute("loggedInUser").toString();
        User user = userService.getUserByUsername(username);
        model.addAttribute("user", user);

        Exam exam = examService.getExam(examId);
        if (exam == null || !exam.getUsername().equals(username)) {
            model.addAttribute("error", "Không tìm thấy đề thi hoặc bạn không có quyền xem thống kê!");
            return "error";
        }

        List<ExamResult> results = examService.getExamResultsByExamId(examId);
        System.out.println("Số kết quả trả về: " + results.size());
        for (ExamResult result : results) {
            System.out.println("Kết quả trong controller: resultId=" + result.getResultId() +
                    ", username=" + result.getUsername() +
                    ", score=" + result.getScore());
        }

        ExamStatistics stats = examService.calculateStatistics(results);

        model.addAttribute("exam", exam);
        model.addAttribute("stats", stats);
        model.addAttribute("results", results);

        return "examStatistics";
    }

    @GetMapping("/exams/liked-page")
    public String showLikedExamsPage(  Model model, HttpServletRequest request, HttpSession session) {
        if (session.getAttribute("loggedInUser") == null) {
            return "redirect:/login"; // Nếu chưa đăng nhập, chuyển hướng về home
        }
        String username = session.getAttribute("loggedInUser").toString();
        User user =userService.getUserByUsername(username);
        model.addAttribute("user", user);
        return "likedExams";
    }

    @GetMapping("/exams/edit/{examId}")
    public String editExam(@PathVariable String examId, Model model, HttpServletRequest request, HttpSession session) {
        Exam exam = examService.getExam(examId);
        if (exam.getUsername().equals(session.getAttribute("loggedInUser"))) {
            model.addAttribute("username", session.getAttribute("loggedInUser"));
            model.addAttribute("exam", exam);
            model.addAttribute("examId", examId);
            return "editExam";
        }
        else {
            return "redirect:/login";
        }
    }
    @ResponseBody
    @PostMapping("/exams/edit/{examId}")
    public ResponseEntity<String> updateExam(@PathVariable String examId, @RequestBody Map<String, Object> examData, HttpSession session, HttpServletRequest request) {
        // Lấy username từ session
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Chưa đăng nhập!");
        }

        // Lấy thông tin user từ UserService
        User user = userService.getUserByUsername(username);
        if (user == null || (user.getRole() != User.Role.ADMIN && user.getRole() != User.Role.TEACHER)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body("Bạn không có quyền sửa đề thi!");
        }

        try {
            examService.addExam(examData, examId);
            return ResponseEntity.ok("Đã cập nhật đề thi thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật đề thi!");

        }
    }


    @PostMapping("/exams/{examId}/feedback")
    public String submitFeedback(
            @PathVariable String examId,
            @RequestParam String content,
            @RequestParam(value = "rate", defaultValue = "0") int rate,
            HttpSession session,
            Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            session.setAttribute("error", "Bạn cần đăng nhập để gửi feedback.");
            return "redirect:/login";
        }
        Exam exam = examService.getExam(examId);
        if (exam == null) {
            session.setAttribute("error", "Không tìm thấy đề thi.");
            return "redirect:/exams";
        }
        // Kiểm tra nếu là người tạo đề
        if (exam.getUsername().equals(username)) {
            session.setAttribute("error", "Người tạo đề không thể gửi feedback riêng.");
            return "redirect:/exams/" + examId + "/detail";
        }
        // Kiểm tra xem người dùng đã gửi feedback chưa
        if (feedbackService.hasUserCommented(examId, username)) {
            session.setAttribute("error", "Bạn chỉ có thể gửi feedback một lần.");
            return "redirect:/exams/" + examId + "/detail";
        }
        User user = userService.getUserByUsername(username);
        String avatarUrl = user != null ? user.getAvatarUrl() : "";
        String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        Feedback feedback = feedbackService.saveFeedback(examId, username, avatarUrl, content, date, rate);
        session.setAttribute(feedback != null ? "success" : "error",
                feedback != null ? "Feedback đã được gửi thành công!" : "Lỗi khi gửi feedback.");
        return "redirect:/exams/" + examId + "/detail";
    }
    @PostMapping("/exams/delete/{examId}")
    public String deleteExam(@PathVariable String examId,
                             HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            session.setAttribute("error", "Bạn cần đăng nhập.");
            return "redirect:/login";
        }
        Exam exam = examService.getExam(examId);
        if (exam != null && exam.getUsername().equals(username)) {
            // Thay vì xoá hoàn toàn, chuyển trạng thái của đề thi thành "DISABLED"
            examService.disableExam(examId);
            session.setAttribute("success", "Đề thi đã được vô hiệu hoá thành công!");
            return "redirect:/exams/created";
        } else {
            model.addAttribute("error", "Bạn không có quyền xoá đề thi này hoặc đề thi không tồn tại!");
            return "error";
        }
    }

    @GetMapping("/exams/{examId}/feedbacks")
    public ResponseEntity<List<Feedback>> getFeedbacks(@PathVariable String examId) {
        List<Feedback> feedbacks = feedbackService.getFeedbacksByExamId(examId);
        return ResponseEntity.ok(feedbacks);
    }

    @PostMapping("/exams/{examId}/feedback/edit")
    public String editFeedback(
            @PathVariable String examId,
            @RequestParam String content,
            @RequestParam(value = "rate", defaultValue = "0") int rate,
            @RequestParam String feedbackId,
            HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            session.setAttribute("error", "Bạn cần đăng nhập để chỉnh sửa feedback.");
            return "redirect:/login";
        }
        List<Feedback> feedbacks = feedbackService.getFeedbacksByExamId(examId);
        Feedback feedbackToUpdate = feedbacks.stream()
                .filter(f -> f.getFeedbackId().equals(feedbackId) && f.getUsername().equals(username))
                .findFirst()
                .orElse(null);
        if (feedbackToUpdate == null) {
            session.setAttribute("error", "Feedback không tồn tại hoặc bạn không có quyền chỉnh sửa.");
            return "redirect:/exams/" + examId + "/detail";
        }
        feedbackToUpdate.setContent(content);
        feedbackToUpdate.setRate(rate);
        feedbackToUpdate.setDate(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        Feedback updatedFeedback = feedbackService.updateFeedback(examId, feedbackToUpdate);
        session.setAttribute(updatedFeedback != null ? "success" : "error",
                updatedFeedback != null ? "Cập nhật feedback thành công!" : "Lỗi khi cập nhật feedback!");
        return "redirect:/exams/" + examId + "/detail";
    }

    @DeleteMapping("/exams/{examId}/feedback/delete/{feedbackId}")
    @ResponseBody
    public ResponseEntity<?> deleteFeedback(
            @PathVariable String examId,
            @PathVariable String feedbackId,
            HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("message", "Bạn cần đăng nhập để xóa feedback."));
        }
        log.info("Xóa feedback - examId: {}, feedbackId: {}, username: {}", examId, feedbackId, username);
        List<Feedback> feedbacks = feedbackService.getFeedbacksByExamId(examId);
        log.info("Danh sách feedbacks: {}", feedbacks);
        Feedback feedbackToDelete = feedbacks.stream()
                .filter(f -> f.getFeedbackId().equals(feedbackId) && f.getUsername().equals(username))
                .findFirst()
                .orElse(null);
        if (feedbackToDelete == null) {
            log.warn("Không tìm thấy feedback với feedbackId: {} và username: {}", feedbackId, username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of("message", "Feedback không tồn tại hoặc bạn không có quyền xóa."));
        }
        try {
            feedbackService.deleteFeedback(examId, feedbackId);
            log.info("Xóa feedback thành công: {}", feedbackId);
            return ResponseEntity.ok(Map.of("message", "Feedback đã xóa thành công!"));
        } catch (Exception e) {
            log.error("Lỗi khi xóa feedback: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("message", "Lỗi khi xóa feedback: " + e.getMessage()));
        }
    }


    @PostMapping("/exams/{examId}/feedback/{feedbackId}/reply")
    public String submitReply(
            @PathVariable String examId,
            @PathVariable String feedbackId,
            @RequestParam String content,
            HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            session.setAttribute("error", "Bạn cần đăng nhập để trả lời feedback.");
            return "redirect:/login";
        }
        if (!userService.isExamCreator(examId, username)) {
            session.setAttribute("error", "Chỉ người tạo đề mới có thể trả lời feedback.");
            return "redirect:/exams/" + examId + "/detail";
        }
        User user = userService.getUserByUsername(username);
        String date = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        String avatarUrl = user != null ? user.getAvatarUrl() : "";
        log.info("Người tạo đề reply feedback: username={}, avatarUrl={}", username, avatarUrl);
        Feedback reply = feedbackService.saveReply(examId, feedbackId, username,avatarUrl, content, date);
        session.setAttribute(reply != null ? "success" : "error",
                reply != null ? "Trả lời feedback thành công!" : "Lỗi khi trả lời feedback.");
        return "redirect:/exams/" + examId + "/detail";
    }

}

