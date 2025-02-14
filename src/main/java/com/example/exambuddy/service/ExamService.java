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

@Service
public class ExamService {
    private Firestore db = FirestoreClient.getFirestore();

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
}
