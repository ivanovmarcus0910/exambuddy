package com.example.exambuddy.controller;

import com.example.exambuddy.model.Question;
import com.example.exambuddy.service.QuestionBankService;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Controller
@RequestMapping("/api/question-bank")
public class QuestionBankController {

    @Autowired
    private QuestionBankService questionBankService;

    @GetMapping("/question-bank")
    public String viewQuestionBank(Model model) {
        return "questionBank";
    }

    @GetMapping("/create-test")
    public String createTestPage() {
        return "create-test";
    }

    // Lấy câu hỏi từ kho private (không thay đổi)
    @GetMapping("/private")
    @ResponseBody
    public ResponseEntity<List<Question>> getPrivateQuestions(HttpSession session,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "20") int limit) {
        try {
            String userId = (String) session.getAttribute("loggedInUser");
            if (userId == null) return ResponseEntity.status(401).body(null);
            List<Question> questions = questionBankService.getPrivateQuestions(userId, "all", "all", "all", "all", "all", page, limit);
            return ResponseEntity.ok(questions);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Lấy câu hỏi từ kho shared (không thay đổi)
    @GetMapping("/shared/{bookType}/{classGrade}/{subjectName}/{chapterName}/{lessonName}")
    @ResponseBody
    public ResponseEntity<List<Question>> getSharedQuestions(
            @PathVariable String bookType,
            @PathVariable String classGrade,
            @PathVariable String subjectName,
            @PathVariable String chapterName,
            @PathVariable String lessonName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            List<Question> questions = questionBankService.getSharedQuestions(bookType, classGrade, subjectName, chapterName, lessonName, page, limit);
            return ResponseEntity.ok(questions);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Lấy câu hỏi từ kho private với bộ lọc (không thay đổi)
    @GetMapping("/private/{bookType}/{classGrade}/{subjectName}/{chapterName}/{lessonName}")
    @ResponseBody
    public ResponseEntity<List<Question>> getPrivateQuestionsWithFilter(
            HttpSession session,
            @PathVariable String bookType,
            @PathVariable String classGrade,
            @PathVariable String subjectName,
            @PathVariable String chapterName,
            @PathVariable String lessonName,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int limit) {
        try {
            String userId = (String) session.getAttribute("loggedInUser");
            if (userId == null) return ResponseEntity.status(401).body(null);
            List<Question> questions = questionBankService.getPrivateQuestions(userId, bookType, classGrade, subjectName, chapterName, lessonName, page, limit);
            return ResponseEntity.ok(questions);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Thêm câu hỏi vào kho shared (không thay đổi)
    @PostMapping("/shared/{bookType}/{classGrade}/{subjectName}/{chapterName}/{lessonName}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addSharedQuestion(
            HttpSession session,
            @PathVariable String bookType,
            @PathVariable String classGrade,
            @PathVariable String subjectName,
            @PathVariable String chapterName,
            @PathVariable String lessonName,
            @RequestBody Question question) {
        try {
            String userId = (String) session.getAttribute("loggedInUser");
            if (userId == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Chưa đăng nhập!");
                return ResponseEntity.status(401).body(response);
            }
            question.setCreatedBy(userId); // Thêm thông tin người tạo
            questionBankService.addSharedQuestion(bookType, classGrade, subjectName, chapterName, lessonName, question);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Thêm vào kho chung thành công!");
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Lỗi khi thêm câu hỏi: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Thêm câu hỏi vào kho private (không thay đổi)
    @PostMapping("/private/{bookType}/{classGrade}/{subjectName}/{chapterName}/{lessonName}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> addPrivateQuestion(
            HttpSession session,
            @PathVariable String bookType,
            @PathVariable String classGrade,
            @PathVariable String subjectName,
            @PathVariable String chapterName,
            @PathVariable String lessonName,
            @RequestBody Question question) {
        try {
            String userId = (String) session.getAttribute("loggedInUser");
            if (userId == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Chưa đăng nhập!");
                return ResponseEntity.status(401).body(response);
            }
            question.setCreatedBy(userId); // Thêm thông tin người tạo
            questionBankService.addPrivateQuestion(userId, bookType, classGrade, subjectName, chapterName, lessonName, question);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Thêm vào kho cá nhân thành công!");
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Lỗi khi thêm câu hỏi: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Đẩy câu hỏi từ private lên shared (endpoint mới)
    @PostMapping("/private-to-shared/{bookType}/{classGrade}/{subjectName}/{chapterName}/{lessonName}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> moveToShared(
            HttpSession session,
            @PathVariable String bookType,
            @PathVariable String classGrade,
            @PathVariable String subjectName,
            @PathVariable String chapterName,
            @PathVariable String lessonName,
            @RequestBody Map<String, List<String>> request) {
        try {
            String userId = (String) session.getAttribute("loggedInUser");
            if (userId == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Chưa đăng nhập!");
                return ResponseEntity.status(401).body(response);
            }
            List<String> questionIds = request.get("questionIds");
            if (questionIds == null || questionIds.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Danh sách câu hỏi trống!");
                return ResponseEntity.badRequest().body(response);
            }
            questionBankService.moveToShared(userId, bookType, classGrade, subjectName, chapterName, lessonName, questionIds);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Đã chia sẻ câu hỏi lên kho chung!");
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Lỗi khi chia sẻ câu hỏi: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Lấy câu hỏi từ shared về private (không thay đổi)
    @PostMapping("/shared-to-private/{bookType}/{classGrade}/{subjectName}/{chapterName}/{lessonName}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> moveToPrivate(
            HttpSession session,
            @PathVariable String bookType,
            @PathVariable String classGrade,
            @PathVariable String subjectName,
            @PathVariable String chapterName,
            @PathVariable String lessonName,
            @RequestBody Map<String, List<String>> request) {
        try {
            String userId = (String) session.getAttribute("loggedInUser");
            if (userId == null) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Chưa đăng nhập!");
                return ResponseEntity.status(401).body(response);
            }
            List<String> questionIds = request.get("questionIds");
            if (questionIds == null || questionIds.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Danh sách câu hỏi trống!");
                return ResponseEntity.badRequest().body(response);
            }
            questionBankService.moveToPrivate(userId, bookType, classGrade, subjectName, chapterName, lessonName, questionIds);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Thêm câu hỏi vào kho cá nhân thành công!");
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            Map<String, String> response = new HashMap<>();
            response.put("error", "Lỗi khi chuyển câu hỏi: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    // Xóa câu hỏi từ kho private (không thay đổi)
    @DeleteMapping("/private/delete/{questionId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> deletePrivateQuestion(HttpSession session, @PathVariable String questionId) {
        Map<String, String> response = new HashMap<>();
        try {
            String userId = (String) session.getAttribute("loggedInUser");
            if (userId == null) {
                response.put("error", "Chưa đăng nhập! Vui lòng đăng nhập để xóa câu hỏi.");
                return ResponseEntity.status(401).body(response);
            }
            questionBankService.deletePrivateQuestion(userId, questionId);
            response.put("message", "Xóa câu hỏi từ kho cá nhân thành công!");
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            response.put("error", "Lỗi khi xóa câu hỏi: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }

    // Cập nhật câu hỏi trong kho private (không thay đổi)
    @PutMapping("/private/update/{questionId}")
    @ResponseBody
    public ResponseEntity<Map<String, String>> updatePrivateQuestion(
            HttpSession session,
            @PathVariable String questionId,
            @RequestBody Question updatedQuestion) {
        Map<String, String> response = new HashMap<>();
        try {
            String userId = (String) session.getAttribute("loggedInUser");
            if (userId == null) {
                response.put("error", "Chưa đăng nhập! Vui lòng đăng nhập để cập nhật câu hỏi.");
                return ResponseEntity.status(401).body(response);
            }

            // Validate nội dung cơ bản
            if (updatedQuestion.getQuestionText() == null || updatedQuestion.getQuestionText().trim().isEmpty()) {
                response.put("error", "Nội dung câu hỏi không được để trống!");
                return ResponseEntity.badRequest().body(response);
            }

            questionBankService.updatePrivateQuestionContent(userId, questionId, updatedQuestion);
            response.put("message", "Cập nhật câu hỏi trong kho cá nhân thành công!");
            return ResponseEntity.ok(response);
        } catch (ExecutionException | InterruptedException e) {
            response.put("error", "Lỗi khi cập nhật câu hỏi: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        } catch (IllegalArgumentException e) {
            response.put("error", e.getMessage());
            return ResponseEntity.status(400).body(response);
        }
    }


    // Tạo đề thi (không thay đổi)
    @PostMapping("/generate-exam")
    @ResponseBody
    public ResponseEntity<List<Question>> generateExam(HttpSession session, @RequestBody Map<String, Object> request) {
        try {
            String userId = (String) session.getAttribute("loggedInUser");
            if (userId == null) return ResponseEntity.status(401).body(null);
            String repoType = (String) request.get("repoType");
            int numQuestions = (int) request.get("numQuestions");
            @SuppressWarnings("unchecked")
            Map<String, Float> difficultyDistribution = (Map<String, Float>) request.get("difficultyDistribution");
            List<Question> exam = questionBankService.generateExam(userId, repoType, numQuestions, difficultyDistribution);
            return ResponseEntity.ok(exam);
        } catch (ExecutionException | InterruptedException e) {
            return ResponseEntity.status(500).body(null);
        }
    }
}