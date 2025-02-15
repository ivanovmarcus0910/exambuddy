package com.example.exambuddy.service;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.Question;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

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
        try {
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
            System.out.println("Size"+questions.size());
            return true;
        }
        catch (Exception e) {
            return false;
        }
    }
    public boolean addExamSession(String examID, String username, long duration) {
        try {
            DocumentReference docRef = db.collection("examSessions").document(username + "_" + examID);
            Map<String, Object> data = new HashMap<>();
            data.put("startTime", System.currentTimeMillis());
            data.put("duration", duration);
            data.put("submitted", false);
            docRef.set(data);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public boolean isSubmitted(String userId, String examId) {
        DocumentReference docRef = db.collection("examSessions").document(userId + "_" + examId);
        try {
            DocumentSnapshot snapshot = docRef.get().get();
            return snapshot.exists() && Boolean.TRUE.equals(snapshot.getBoolean("submitted"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public void saveProgress(String userId, String examId, Map<String, Object> answers) {
        DocumentReference docRef = db.collection("examProgress").document(userId + "_" + examId);
        docRef.set(Map.of("answers", answers), SetOptions.merge());
    }
    public Map<String, Object> getProgress(String userId, String examId) {
        DocumentReference docRef = db.collection("examProgress").document(userId + "_" + examId);
        try {
            DocumentSnapshot snapshot = docRef.get().get();
            return snapshot.exists() ? snapshot.getData() : new HashMap<>();
        } catch (Exception e) {
            e.printStackTrace();
            return new HashMap<>();
        }
    }
    public long getRemainingTime(String userId, String examId) {
        System.out.println("Ở service : userId = " + userId + ", examId = " + examId);
        DocumentReference docRef = db.collection("examSessions").document(userId + "_" + examId);
        try {
            DocumentSnapshot snapshot = docRef.get().get();
            if (snapshot.exists()) {
                long startTime = snapshot.getLong("startTime");
                long duration = snapshot.getLong("duration");
                long currentTime = System.currentTimeMillis();
                System.out.println("start:"+startTime+" duration:"+duration+" currentTime:"+currentTime);
                return Math.max(0, (startTime + duration) - currentTime);
            }
            System.out.println("Có vào đây");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }
    public void submitExam(String userId, String examId) {
        DocumentReference docRef = db.collection("examSessions").document(userId + "_" + examId);
        docRef.update("submitted", true);
    }
    public void saveExamResult(String examId, double score, Map<String, List<Integer>> userAnswers) {
        DocumentReference docRef = db.collection("examResults").document(examId);
        docRef.set(Map.of(
                "score", score,
                "answers", userAnswers,
                "submittedAt", System.currentTimeMillis()
        ), SetOptions.merge());
    }
}
