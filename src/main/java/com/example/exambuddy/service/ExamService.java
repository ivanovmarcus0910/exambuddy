package com.example.exambuddy.service;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.ExamResult;
import com.example.exambuddy.model.Question;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.text.SimpleDateFormat;
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

                // Đếm số câu hỏi cho mỗi đề thi và gán vào exam
                int questionCount = countQuestions(exam.getExamID());
                exam.setQuestionCount(questionCount);
                // Chuyển đổi và format ngày
                if (exam.getDate() != null) {
                    String formattedDate = formatDate(exam.getDate());
                    exam.setDate(formattedDate);
                }

                exams.add(exam);
            }
        } catch (Exception e) {
            e.printStackTrace(); // Bạn có thể log lỗi ở đây
        }
        return exams;
    }


    // Phương thức đếm số câu hỏi trong subcollection "questions"
    public int countQuestions(String examId) throws Exception {
        ApiFuture<QuerySnapshot> query = db.collection("exams")
                .document(examId)
                .collection("questions")
                .get();
        List<QueryDocumentSnapshot> questionsSnapshot = query.get().getDocuments();
        return questionsSnapshot.size();
    }




    // Phương thức format lại ngày
    public String formatDate(String dateString) {
        try {
            // Định dạng chuỗi Firebase: yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            // Định dạng ngày muốn hiển thị: dd/MM/yyyy
            SimpleDateFormat targetFormat = new SimpleDateFormat("dd/MM/yyyy");

            // Chuyển chuỗi thành Date
            Date date = originalFormat.parse(dateString);
            return targetFormat.format(date); // Trả về ngày đã định dạng
        } catch (Exception e) {
            e.printStackTrace();
            return null; // Trả về null nếu có lỗi
        }
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
            if (docRef.get().get().exists()) {
                System.out.println("Session đã tồn tại, không thêm mới.");
                return false;
            }
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
                System.out.println("start:"+startTime+" duration:"+duration+" currentTime:"+currentTime+" timeremaining "+((startTime + duration) - currentTime));
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
        docRef.delete();
        docRef = db.collection("examProgress").document(userId + "_" + examId);
        docRef.delete();
    }
    public void saveExamResult(String userId, String examId, double score, Exam exam, MultiValueMap<String, String> userAnswers, List<String> correctAnswers) {
        String ResultID = UUID.randomUUID().toString();
        DocumentReference docRef = db.collection("examResults").document(userId + "_" + examId+"_"+ResultID);
        docRef.set(Map.of(
                "examID",examId,
                "score", score,
                "answers", userAnswers,
                "submittedAt", System.currentTimeMillis(),
                "correctAnswers", correctAnswers

        ), SetOptions.merge());
    }
    public  List<ExamResult> getExamResultByUsername(String userId) {
        List<ExamResult> results = new ArrayList<>();

        try {
            CollectionReference collectionReference = db.collection("examResults");
            ApiFuture<QuerySnapshot> future = collectionReference
                    .whereGreaterThanOrEqualTo(FieldPath.documentId(), userId + "_")
                    .whereLessThan(FieldPath.documentId(), userId + "_\uf8ff")
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (DocumentSnapshot doc : documents) {
                ExamResult examResult = doc.toObject(ExamResult.class);
                results.add(examResult);
            }
        }
        catch (Exception e) {
        }
        return  results;
    }

    public void likeExam(String userId, String examId) {
        DocumentReference docRef = db.collection("likedExams").document(userId + "_" + examId);
        docRef.set(Map.of(
                "userId", userId,
                "examId", examId,
                "likedAt", System.currentTimeMillis()
        ), SetOptions.merge());
    }

    public List<Exam> getLikedExamsByUser(String userId) {
        List<Exam> likedExams = new ArrayList<>();

        try {
            CollectionReference collectionReference = db.collection("likedExams");
            ApiFuture<QuerySnapshot> future = collectionReference
                    .whereEqualTo("userId", userId) // Tìm bài thi đã thích theo userId
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            List<String> examIds = new ArrayList<>();

            for (DocumentSnapshot doc : documents) {
                examIds.add(doc.getString("examId"));
            }

            // Lấy thông tin bài thi từ danh sách examId
            CollectionReference examCollection = db.collection("exams");
            for (String id : examIds) {
                DocumentSnapshot examDoc = examCollection.document(id).get().get();
                if (examDoc.exists()) {
                    Exam exam = examDoc.toObject(Exam.class);
                    exam.setExamID(examDoc.getId()); // Đặt examID trực tiếp từ ID tài liệu
                    likedExams.add(exam);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return likedExams;
    }

    public List<Exam> getHtoryCreateExamsByUsername(String username) {
        List<Exam> exams = new ArrayList<>();

        try {
            CollectionReference collectionReference = db.collection("exams");
            ApiFuture<QuerySnapshot> future = collectionReference
                    .whereEqualTo("username", username) // Tìm bài thi theo username
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (DocumentSnapshot doc : documents) {
                if (doc.exists()) {
                    Exam exam = doc.toObject(Exam.class);
                    exam.setExamID(doc.getId());
                    if (exam.getDate() != null) {
                        String formattedDate = formatDate(exam.getDate());
                        exam.setDate(formattedDate);
                    }// Đặt examID trực tiếp từ ID tài liệu
                    exams.add(exam);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return exams;
    }

}
