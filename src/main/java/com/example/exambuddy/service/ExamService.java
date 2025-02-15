package com.example.exambuddy.service;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.Question;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ExamService {
    private final Firestore db = FirestoreClient.getFirestore();

    public List<Exam> getExamList() {
        List<Exam> exams = new ArrayList<>();

        try {
            ApiFuture<QuerySnapshot> future = db.collection("exams").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot doc : documents) {
                Exam exam = doc.toObject(Exam.class);
                exam.setExamID(doc.getId()); // Gán ID Firestore vào Exam
                exams.add(exam);
            }
        } catch (Exception e) {

        }
        return exams;
    }

    public Exam getExam(String examID) {
        try {
            DocumentReference examRef = db.collection("exams").document(examID);
            DocumentSnapshot document = examRef.get().get();

            if (!document.exists()) {
                return null;
            }
            Exam exam = document.toObject(Exam.class);
             exam.setExamID(document.getId());
            List<Question> questions = new ArrayList<>();
            ApiFuture<QuerySnapshot> future = db.collection("exams").document(examID).collection("questions").get();
            List<QueryDocumentSnapshot> questionDocs = future.get().getDocuments();

            for (QueryDocumentSnapshot questionDoc : questionDocs) {
                Question question = questionDoc.toObject(Question.class);
                questions.add(question);
            }
            exam.setQuestions(questions);
            return exam;
        } catch (Exception e) {
            return null;
        }

    }
    public boolean addExam(Map<String, Object> examData) {
        System.out.println("Bắt đầu add đề");
        try {
            String examId = UUID.randomUUID().toString();
            System.out.println("examID:"+examId);
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
            System.out.println("Size"+questions.size());
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
}
