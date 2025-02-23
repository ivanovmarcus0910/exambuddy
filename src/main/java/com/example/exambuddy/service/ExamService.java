package com.example.exambuddy.service;

import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.ExamResult;
import com.example.exambuddy.model.Question;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;

import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.ExecutionException;
@Service
public class ExamService {
    private final Firestore db = FirestoreClient.getFirestore();

    public List<Exam> getExamList(int page, int size) {
        List<Exam> exams = new ArrayList<>();
        try {
            // Lấy danh sách theo thứ tự mới nhất và giới hạn số lượng theo trang
            Query query = db.collection("exams").orderBy("date", Query.Direction.DESCENDING).limit(size);

            if (page > 0) {
                ApiFuture<QuerySnapshot> previousFuture = db.collection("exams")
                        .orderBy("date", Query.Direction.DESCENDING)
                        .limit(size * page).get();

                List<QueryDocumentSnapshot> previousDocs = previousFuture.get().getDocuments();
                if (!previousDocs.isEmpty()) {
                    query = query.startAfter(previousDocs.get(previousDocs.size() - 1));
                }
            }

            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot doc : documents) {
                Exam exam = doc.toObject(Exam.class);
                exam.setExamID(doc.getId());
                exam.setQuestionCount(countQuestions(exam.getExamID()));

                if (exam.getDate() != null) {
                    exam.setDate(formatDate(exam.getDate()));
                }
                exams.add(exam);
            }
        } catch (Exception e) {
            e.printStackTrace();
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
    public static String formatDate(String dateString) {
        try {
            // Định dạng ban đầu từ Firebase (luôn là UTC)
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            originalFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            // Chuyển chuỗi Firebase thành đối tượng Date (mặc định là UTC)
            Date date = originalFormat.parse(dateString);
            System.out.println("Parsed Date (UTC): " + date); // Debug

            // Chuyển sang múi giờ hệ thống mà không làm sai lệch giá trị
            SimpleDateFormat targetFormat = new SimpleDateFormat("dd/MM/yyyy");
            return targetFormat.format(new Date(date.getTime())); // Chuyển đổi về đúng format
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi định dạng ngày!";
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
                    "grade", examData.get("grade"),
                    "subject", examData.get("subject"),
                    "examType", examData.get("examType"),
                    "city", examData.get("city"),
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
        String idRandom = UUID.randomUUID().toString();
        String ResultsId = userId + "_" + examId + "_" + idRandom;
        DocumentReference docRef = db.collection("examResults").document(ResultsId);
        docRef.set(Map.of(
                "resultId",ResultsId,
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
                System.out.println("Document data: " + doc.getData());
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

    public ExamResult getExamResult(String resultId) {
        Firestore db = FirestoreClient.getFirestore();
        try {
            String documentId = resultId;
            System.out.println("Fetching document with ID: " + documentId); // Log ID tài liệu

            // Lấy document có ID = "username_examId"
            DocumentSnapshot doc = db.collection("examResults")
                    .document(documentId)
                    .get()
                    .get();

            if (doc.exists()) {
                System.out.println("Document data: " + doc.getData()); // Log dữ liệu tài liệu
                return doc.toObject(ExamResult.class); // Chuyển dữ liệu Firestore thành Java Object
            } else {
                System.out.println("Document does not exist!"); // Log nếu tài liệu không tồn tại
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return null;
    }

    private String normalizeString(String input) {
        if (input == null) {
            return "";
        }
        // Chuyển về chữ thường và xử lý ký tự đặc biệt
        return Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "") // Loại bỏ dấu
                .toLowerCase();           // Chuyển về chữ thường
    }

    public List<Exam> searchExamByName(String examName) throws ExecutionException, InterruptedException {
        List<Exam> exams = new ArrayList<>();
        String normalizedSearchTerm = normalizeString(examName); // Chuẩn hóa chuỗi tìm kiếm

        ApiFuture<QuerySnapshot> querySnapshot = db.collection("exams").get(); // Lấy tất cả đề thi từ Firestore
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

        for (QueryDocumentSnapshot doc : documents) {
            Exam exam = doc.toObject(Exam.class);
            String normalizedExamName = normalizeString(exam.getExamName()); // Chuẩn hóa tên đề thi

            // Kiểm tra xem chuỗi tìm kiếm có nằm trong tên đề thi hay không
            if (normalizedExamName.contains(normalizedSearchTerm)) {
                exam.setExamID(doc.getId());

                try {
                    int questionCount = countQuestions(exam.getExamID());
                    exam.setQuestionCount(questionCount);
                } catch (Exception e) {
                    e.printStackTrace();
                    exam.setQuestionCount(0); // Gán giá trị mặc định nếu có lỗi
                }

                if (exam.getDate() != null) {
                    String formattedDate = formatDate(exam.getDate());
                    exam.setDate(formattedDate);
                }

                exams.add(exam); // Thêm đề thi vào danh sách kết quả
            }
        }

        return exams; // Trả về danh sách đề thi phù hợp
    }

    public List<Exam> searchExamsByFilter(String grade, String subject, String examType, String city) {
        List<Exam> resultList = new ArrayList<>();
        Firestore db = FirestoreClient.getFirestore();

        try {
            CollectionReference examsRef = db.collection("exams");
            Query query = examsRef;
            System.out.println("[Firestore Query] examType: " + examType); // Thêm dòng này
            if (!examType.isEmpty()) {
                query = query.whereEqualTo("examType", examType);
            }
            // Thêm điều kiện lọc cho từng tham số (nếu có giá trị)
            if (!grade.isEmpty()) {
                query = query.whereEqualTo("grade", grade); // Lọc theo lớp
            }
            if (!subject.isEmpty()) {
                query = query.whereEqualTo("subject", subject); // Lọc theo môn học
            }
            if (!examType.isEmpty()) {
                query = query.whereEqualTo("examType", examType); // Lọc theo loại đề
            }
            if (!city.isEmpty()) {
                query = query.whereEqualTo("city", city); // Lọc theo thành phố
            }

            // Thực hiện truy vấn
            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            // Xử lý kết quả
            for (QueryDocumentSnapshot doc : documents) {
                Exam exam = doc.toObject(Exam.class);
                exam.setExamID(doc.getId());
                int questionCount = countQuestions(exam.getExamID());
                exam.setQuestionCount(questionCount);
                if (exam.getDate() != null) {
                    String formattedDate = formatDate(exam.getDate());
                    exam.setDate(formattedDate);
                }
                resultList.add(exam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return resultList;
    }
}
