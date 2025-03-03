package com.example.exambuddy.service;


import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.ExamResult;
import com.example.exambuddy.model.Question;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.common.collect.Table;
import com.google.firebase.cloud.FirestoreClient;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;


import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;
import java.io.InputStream;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.Date;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
            System.out.println("Creating exam with ID: " + examId);
            Object tags = examData.get("tags");
            if (tags instanceof String[]) {
                examData.put("tags", Arrays.asList((String[]) tags));
            }
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
            System.out.println("Exam document set");


            // Thêm danh sách câu hỏi vào subcollection "questions"
            // Thêm danh sách câu hỏi vào subcollection "questions"
            List<Map<String, Object>> questions = (List<Map<String, Object>>) examData.get("questions");
            WriteBatch batch = db.batch();
            for (Map<String, Object> question : questions) {
                String questionId = UUID.randomUUID().toString();
                batch.set(db.collection("exams").document(examId).collection("questions").document(questionId), question);
            }
            batch.commit().get();
            System.out.println("Questions batch committed");
            return true;
        } catch (Exception e) {
            System.out.println("Error in addExam: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }


    public boolean importExamFromExcel(InputStream inputStream, Map<String, Object> examData) {
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            Sheet sheet = workbook.getSheetAt(0); // Lấy sheet đầu tiên


            // Tạo danh sách câu hỏi từ các dòng trong Excel
            List<Map<String, Object>> questions = new ArrayList<>();
            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;


                Map<String, Object> questionData = new HashMap<>();
                questionData.put("questionText", row.getCell(0).getStringCellValue());


                // Lấy các lựa chọn (cột 1-4: A, B, C, D)
                List<String> options = new ArrayList<>();
                for (int j = 1; j <= 4; j++) {
                    Cell cell = row.getCell(j);
                    options.add(cell != null ? cell.getStringCellValue() : "");
                }
                questionData.put("options", options);


                // Lấy đáp án đúng (cột 5, dạng chuỗi: "A", "B", "C", "D" hoặc nhiều đáp án cách nhau bởi dấu phẩy)
                List<Integer> correctAnswers = new ArrayList<>();
                String correctAnswerStr = row.getCell(5).getStringCellValue().toUpperCase(); // Chuyển thành chữ in hoa
                for (String answer : correctAnswerStr.split(",")) {
                    switch (answer.trim()) {
                        case "A":
                            correctAnswers.add(0);
                            break;
                        case "B":
                            correctAnswers.add(1);
                            break;
                        case "C":
                            correctAnswers.add(2);
                            break;
                        case "D":
                            correctAnswers.add(3);
                            break;
                        default:
                            throw new IllegalArgumentException("Đáp án không hợp lệ: " + answer);
                    }
                }
                questionData.put("correctAnswers", correctAnswers);


                questions.add(questionData);
            }


            // Thêm danh sách câu hỏi vào examData
            examData.put("questions", questions);
            examData.put("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                    .format(new Date()));


            // Lưu dữ liệu vào Firestore
            return addExam(examData);


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
    public boolean importExamFromDocx(InputStream inputStream, Map<String, Object> examData) {
        try {
            XWPFDocument doc = new XWPFDocument(inputStream);
            List<Map<String, Object>> questions = new ArrayList<>();


            // Trích xuất câu hỏi
            StringBuilder text = new StringBuilder();
            for (XWPFParagraph para : doc.getParagraphs()) {
                String line = para.getText().trim();
                if (!line.isEmpty()) {
                    text.append(line).append("\n");
                }
            }


            String questionsText = text.toString().split("-----------HẾT----------")[0].trim();


            // Trích xuất câu hỏi và đáp án
            Pattern questionPattern = Pattern.compile(
                    "Câu \\d+:(.+?)\\s*A\\.(.+?)\\s*B\\.(.+?)\\s*C\\.(.+?)\\s*D\\.(.+?)\\s*(?=Câu|$)",
                    Pattern.DOTALL
            );
            Matcher questionMatcher = questionPattern.matcher(questionsText);
            while (questionMatcher.find()) {
                Map<String, Object> questionData = new HashMap<>();
                questionData.put("questionText", questionMatcher.group(1).trim());
                List<String> options = Arrays.asList(
                        questionMatcher.group(2).trim(),
                        questionMatcher.group(3).trim(),
                        questionMatcher.group(4).trim(),
                        questionMatcher.group(5).trim()
                );
                questionData.put("options", options);
                questionData.put("correctAnswers", new ArrayList<>());
                questions.add(questionData);
            }


            // Tìm bảng đáp án dựa trên nội dung (không phụ thuộc vào "HẾT")
            XWPFTable answerTable = null;
            for (XWPFTable table : doc.getTables()) {
                boolean isAnswerTable = false;
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText().trim();
                        // Kiểm tra nếu ô chứa "ĐÁP ÁN" hoặc định dạng câu trả lời (ví dụ: 1.C, 2.A)
                        if (cellText.contains("ĐÁP ÁN") || cellText.matches(".*\\d+\\s*[.\\s]\\s*[A-D].*")) {
                            isAnswerTable = true;
                            break;
                        }
                    }
                    if (isAnswerTable) break;
                }
                if (isAnswerTable) {
                    answerTable = table;
                    break;
                }
            }


            if (answerTable == null) {
                throw new Exception("Không tìm thấy bảng đáp án trong file.");
            }


            // Trích xuất đáp án từ bảng
            Map<Integer, String> answerMap = new HashMap<>();
            for (XWPFTableRow row : answerTable.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    String cellText = cell.getText().trim();
                    Pattern answerPattern = Pattern.compile("(\\d+)[.\\s]*([A-D])");
                    Matcher answerMatcher = answerPattern.matcher(cellText);
                    while (answerMatcher.find()) {
                        int qIndex = Integer.parseInt(answerMatcher.group(1).trim()) - 1;
                        String correctAnswer = answerMatcher.group(2).trim();
                        answerMap.put(qIndex, correctAnswer);
                    }
                }
            }


            // Gán đáp án cho câu hỏi
            for (int i = 0; i < questions.size(); i++) {
                if (answerMap.containsKey(i)) {
                    String correctAnswer = answerMap.get(i);
                    List<Integer> correctAnswers = (List<Integer>) questions.get(i).get("correctAnswers");
                    correctAnswers.clear();
                    switch (correctAnswer) {
                        case "A": correctAnswers.add(0); break;
                        case "B": correctAnswers.add(1); break;
                        case "C": correctAnswers.add(2); break;
                        case "D": correctAnswers.add(3); break;
                    }
                    questions.get(i).put("correctAnswers", correctAnswers);
                }
            }


            examData.put("questions", questions);
            examData.put("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date()));
            return addExam(examData);


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }


    public boolean addExamSession(String examID, String username, long duration) {
        try {
            DocumentReference docRef = db.collection("examSessions").document(username + "_" + examID);
            if (docRef.get().get().exists()) {
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
        DocumentReference docRef = db.collection("examSessions").document(userId + "_" + examId);
        try {
            DocumentSnapshot snapshot = docRef.get().get();
            if (snapshot.exists()) {
                long startTime = snapshot.getLong("startTime");
                long duration = snapshot.getLong("duration");
                long currentTime = System.currentTimeMillis();
                return Math.max(0, (startTime + duration) - currentTime);
            }
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
                "resultId", ResultsId,
                "examID", examId,
                "score", score,
                "answers", userAnswers,
                "submittedAt", System.currentTimeMillis(),
                "correctAnswers", correctAnswers

        ), SetOptions.merge());
    }

    public List<ExamResult> getExamResultByUsername(String userId) {
        List<ExamResult> results = new ArrayList<>();

        try {
            CollectionReference collectionReference = db.collection("examResults");

            // Tạo truy vấn với điều kiện lọc và sắp xếp theo ngày giảm dần
            Query query = collectionReference
                    .whereGreaterThanOrEqualTo(FieldPath.documentId(), userId + "_")
                    .whereLessThan(FieldPath.documentId(), userId + "_\uf8ff")
                    .orderBy("submittedAt", Query.Direction.DESCENDING); // Sắp xếp theo submittedAt giảm dần (mới nhất trước)

            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot doc : documents) {
                ExamResult examResult = doc.toObject(ExamResult.class);
                results.add(examResult);
            }
        } catch (Exception e) {
            // Thêm log chi tiết để debug nếu cần
            System.err.println("Lỗi khi lấy kết quả bài thi cho userId: " + userId + " - " + e.getMessage());
        }
        return results;
    }


    public void likeExam(String userId, String examId) {
        DocumentReference docRef = db.collection("likedExams").document(userId + "_" + examId);
        docRef.set(Map.of(
                "userId", userId,
                "examId", examId,
                "likedAt", System.currentTimeMillis()
        ), SetOptions.merge());
    }

    public void unlikeExam(String userId, String examId) {
        DocumentReference docRef = db.collection("likedExams").document(userId + "_" + examId);
        docRef.delete(); // Xóa like khỏi Firestore
    }

    public boolean isExamLiked(String userId, String examId) {
        DocumentReference docRef = db.collection("likedExams").document(userId + "_" + examId);
        try {
            return docRef.get().get().exists();
        } catch (Exception e) {
            return false;
        }
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
                    exam.setQuestionCount(countQuestions(exam.getExamID()));

                    if (exam.getDate() != null) {
                        exam.setDate(formatDate(exam.getDate()));
                    }
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

            // Lấy document có ID = "username_examId"
            DocumentSnapshot doc = db.collection("examResults")
                    .document(documentId)
                    .get()
                    .get();

            if (doc.exists()) {
                return doc.toObject(ExamResult.class); // Chuyển dữ liệu Firestore thành Java Object
            } else {
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

            // Áp dụng các điều kiện lọc nếu có giá trị
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
    // Phương thức lấy all đề thi

    public List<Exam> getAllExams() {
        Firestore firestore = FirestoreClient.getFirestore();
        List<Exam> examList = new ArrayList<>();
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("exams").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (QueryDocumentSnapshot doc : documents) {
                Exam exam = doc.toObject(Exam.class);
                exam.setExamID(doc.getId()); // Thêm dòng này để thiết lập ID cho đề thi
                examList.add(exam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return examList;
    }

    // Xoá đề thi
    public void deleteExam(String examId) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            firestore.collection("exams").document(examId).delete();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // --- Phương thức mới: cập nhật trạng thái active của Exam ---
    public void updateExamStatus(String examId, boolean newStatus) {
        DocumentReference examRef = db.collection("exams").document(examId);
        try {
            examRef.update("active", newStatus).get();
            System.out.println("Cập nhật trạng thái active của exam " + examId + " thành " + newStatus);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Lỗi cập nhật trạng thái của exam " + examId);
            e.printStackTrace();
        }
    }
}
