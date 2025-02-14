package com.example.exambuddy.controller;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.Question;
import com.example.exambuddy.service.ExamService;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.ui.Model;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Map;

@Controller
@RequestMapping("/exams")
public class ManageExamController {
    private Firestore db = FirestoreClient.getFirestore();

    @GetMapping
    public String listExams(Model model) {
        try {
            ExamService eS = new ExamService();
            List<Exam> exams = eS.getExamList();
            model.addAttribute("exams", exams);
            return "examList"; // Trả về trang hiển thị danh sách đề thi
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải danh sách đề thi: " + e.getMessage());
            return "error";
        }
    }

    @GetMapping("/{examId}")
    public String getExam(@PathVariable String examId, Model model) {
        try {
            DocumentReference examRef = db.collection("exams").document(examId);
            DocumentSnapshot document = examRef.get().get();

            if (!document.exists()) {
                model.addAttribute("error", "Đề thi không tồn tại!");
                return "error";
            }

            Exam exam = document.toObject(Exam.class);
            model.addAttribute("exam", exam);
            return "examDetail"; // Trả về trang HTML hiển thị đề thi
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi tải đề thi: " + e.getMessage());
            return "error";
        }
    }
//    @PostMapping("/{examId}/submit")
//    public String submitExam(@PathVariable String examId, @RequestParam Map<String, String> answers, Model model) {
//        try {
//            DocumentReference examRef = db.collection("exams").document(examId);
//            DocumentSnapshot document = examRef.get().get();
//
//            if (!document.exists()) {
//                model.addAttribute("error", "Đề thi không tồn tại!");
//                return "error";
//            }
//
//            Exam exam = document.toObject(Exam.class);
//            List<Question> questions = exam.getQuestions();
//
//            int score = 0;
//            for (int i = 0; i < questions.size(); i++) {
//                String answerKey = "q" + i;
//                if (answers.containsKey(answerKey) && answers.get(answerKey) != null) {
//                    try {
//                        int userAnswer = Integer.parseInt(answers.get(answerKey));
//                        int correctAnswer = Integer.parseInt(questions.get(i).getCorrectAnswers()); // Nếu đúng là String
//
//                        if (userAnswer == correctAnswer) {
//                            score++;
//                        }
//                    } catch (NumberFormatException e) {
//                        System.out.println("Lỗi: Đáp án không hợp lệ - " + answers.get(answerKey));
//                    }
//                }
//            }
//
//            model.addAttribute("exam", exam);
//            model.addAttribute("score", score);
//            model.addAttribute("totalQuestions", questions.size());
//
//            return "examResult";
//        } catch (Exception e) {
//            model.addAttribute("error", "Lỗi khi chấm điểm: " + e.getMessage());
//            return "error";
//        }
//    }

    @GetMapping("/{id}/detail")
    public String viewExamDetail(@PathVariable String id, Model model) {
        try {
            DocumentSnapshot document = db.collection("exams").document(id).get().get();
            if (!document.exists()) {
                model.addAttribute("error", "Đề thi không tồn tại!");
                return "error";
            }

            // Chuyển dữ liệu document thành đối tượng Exam
            Exam exam = document.toObject(Exam.class);
            exam.setExamID(document.getId());

            // Lấy danh sách câu hỏi từ subcollection "questions"
            List<Question> questions = new ArrayList<>();
            ApiFuture<QuerySnapshot> future = db.collection("exams").document(id).collection("questions").get();
            List<QueryDocumentSnapshot> questionDocs = future.get().getDocuments();

            for (QueryDocumentSnapshot questionDoc : questionDocs) {
                Question question = questionDoc.toObject(Question.class);
                questions.add(question);
            }

            exam.setQuestions(questions);
            model.addAttribute("exam", exam);
            model.addAttribute("questions", questions); // Gửi danh sách câu hỏi qua view

            return "examDetail"; // Trả về trang chi tiết đề thi
        } catch (Exception e) {
            model.addAttribute("error", "Lỗi khi lấy đề thi: " + e.getMessage());
            return "error";
        }
    }


    @GetMapping("/addExam")
    public String addQuestionPage() {
        return "addExam.html";
    }

    @PostMapping("/addExam")
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

