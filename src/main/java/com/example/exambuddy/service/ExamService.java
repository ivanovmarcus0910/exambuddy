package com.example.exambuddy.service;


import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.ExamResult;
import com.example.exambuddy.model.ExamStatistics;
import com.example.exambuddy.model.Question;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.cloud.firestore.*;
import com.google.common.collect.Table;
import com.google.firebase.cloud.FirestoreClient;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import java.io.InputStream;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.Date;
import java.util.TimeZone;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class ExamService {
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final Firestore db = FirestoreClient.getFirestore();
    private final LeaderBoardService leaderBoardService = new LeaderBoardService();
    public List<Exam> getExamList(int page, int size) {
        List<Exam> exams = new ArrayList<>();
        try {
            // Lấy danh sách theo thứ tự mới nhất và giới hạn số lượng theo trang
            Query query = db.collection("exams").orderBy("participantCount", Query.Direction.DESCENDING).limit(size);
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
                System.out.println(exam.isActive());
                if (exam.isActive()){
                exam.setExamID(doc.getId());
                exams.add(exam);}
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return exams;
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

    public boolean addExam(Map<String, Object> examData, String id) {
        try {
            String examId;
            if (id.isEmpty()){
                examId = UUID.randomUUID().toString();
            }
            else {
                examId = id;
                DocumentReference examRef = db.collection("exams").document(examId);
                ApiFuture<DocumentSnapshot> future = examRef.get();
                DocumentSnapshot document = future.get();
                if (document.exists()) {
                    CollectionReference questionsRef = examRef.collection("questions");
                    ApiFuture<QuerySnapshot> questionsQuery = questionsRef.get();
                    for (DocumentSnapshot questionDoc : questionsQuery.get().getDocuments()) {
                        questionDoc.getReference().delete();
                    }
                    examRef.delete().get();
                }
            }
            System.out.println("Creating exam with ID: " + examId);
            Object tags = examData.get("tags");
            if (tags instanceof String[]) {
                examData.put("tags", Arrays.asList((String[]) tags));
            }
            List<Map<String, Object>> questions = (List<Map<String, Object>>) examData.get("questions");
            int x = (questions != null) ? questions.size() : 0;
            Object timedurationObj = examData.get("timeduration");
            String timedurationStr = timedurationObj instanceof Integer ? timedurationObj.toString() : (String) timedurationObj;

            // Tạo một HashMap để lưu dữ liệu đề thi
            Map<String, Object> examDataMap = new HashMap<>();
            examDataMap.put("examName", examData.get("examName"));
            examDataMap.put("grade", examData.get("grade"));
            examDataMap.put("subject", examData.get("subject"));
            examDataMap.put("examType", examData.get("examType"));
            examDataMap.put("city", examData.get("city"));
            examDataMap.put("tags", examData.get("tags"));
            examDataMap.put("username", examData.get("username"));
            examDataMap.put("date", examData.get("date"));
            examDataMap.put("timeduration", Long.parseLong(timedurationStr));
            examDataMap.put("questionCount", x);
            examDataMap.put("participantCount", 0); // Mặc định là 0 khi tạo mới

// Thêm dữ liệu vào collection "exams"
            db.collection("exams").document(examId).set(examDataMap).get();
            System.out.println("Exam document set");


            // Thêm danh sách câu hỏi vào subcollection "questions"
            // Thêm danh sách câu hỏi vào subcollection "questions"
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
            return addExam(examData,"");


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
                    }
                    questions.get(i).put("correctAnswers", correctAnswers);
                }
            }


            examData.put("questions", questions);
            examData.put("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date()));
            return addExam(examData,"");


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
            if (snapshot.exists()) {
                if (snapshot.getBoolean("submitted") == true) {
                    return true;
                }
                ;
            }
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
        db.collection("exams").document(examId)
                .update("participantCount", FieldValue.increment(1));
    }
    public void deleteProcess(String userId, String examId) {
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
                "examName", exam.getExamName(),
                "score", score,
                "answers", userAnswers,
                "submittedAt", System.currentTimeMillis(),
                "correctAnswers", correctAnswers,
                "username", userId,
                "exam",exam

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
        try {
            // 1. Lấy thông tin bài thi từ collection "exams"
            DocumentReference examRef = db.collection("exams").document(examId);
            DocumentSnapshot examSnapshot = examRef.get().get();

            if (!examSnapshot.exists()) {
                System.out.println("Bài thi không tồn tại: " + examId);
                return;
            }

            // 2. Lấy các trường cần thiết từ bài thi
            Map<String, Object> examData = examSnapshot.getData();
            // Giả sử các trường được lưu trữ với các tên sau:
            String examName = (String) examData.get("examName");
            String subject = (String) examData.get("subject");
            String grade = (String) examData.get("grade");
            String createdDate = (String) examData.get("date"); // Có thể là timestamp hoặc string tùy thiết kế

            // 3. Tạo map dữ liệu để lưu vào likedExams
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("username", userId);
            likeData.put("examID", examId);
            likeData.put("examName", examName);
            likeData.put("subject", subject);
            likeData.put("grade", grade);
            likeData.put("date", createdDate);

            // 4. Lưu vào collection "likedExams"
            // Bạn có thể để Firestore tự sinh doc ID hoặc dùng userId_examId
            DocumentReference docRef = db.collection("likedExams").document(userId + "_" + examId);
            docRef.set(likeData, SetOptions.merge());

            System.out.println("Like thành công cho exam " + examId);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void unlikeExam(String userId, String examId) {
        DocumentReference docRef = db.collection("likedExams").document(userId + "_" + examId);
        docRef.delete(); // Xóa like khỏi Firestore
    }

    public CompletableFuture<Boolean> isExamLikedAsync(String userId, String examId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                DocumentSnapshot snapshot = db.collection("likedExams")
                        .document(userId + "_" + examId)
                        .get()
                        .get(); // Chờ kết quả trong luồng khác
                return snapshot.exists();
            } catch (Exception e) {
                return false;
            }
        }, executor);
    }

    public List<Exam> getLikedExamsByUser(String userId) {
        List<Exam> likedExams = new ArrayList<>();
        try {
            // Truy vấn collection "likedExams" theo trường "userId"
            CollectionReference likedExamsCollection = db.collection("likedExams");
            ApiFuture<QuerySnapshot> future = likedExamsCollection
                    .whereEqualTo("username", userId)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (DocumentSnapshot doc : documents) {
                // Chuyển document sang đối tượng Exam.
                // Lưu ý: Đảm bảo lớp Exam có các trường phù hợp với dữ liệu đã lưu (examName, subject, grade, createdDate, examId, ...)
                Exam exam = doc.toObject(Exam.class);
                // Nếu cần, có thể set examID từ field examId hoặc từ document id
                if (exam.isActive()){
                exam.setExamID(doc.getString("examID"));
                likedExams.add(exam);}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return likedExams;
    }


    /**
     * Hàm helper để chia danh sách thành các nhóm nhỏ có kích thước batchSize
     */
    private <T> List<List<T>> splitList(List<T> list, int batchSize) {
        List<List<T>> batches = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            int end = Math.min(i + batchSize, list.size());
            batches.add(list.subList(i, end));
        }
        return batches;
    }

    public List<Exam> getHtoryCreateExamsByUsername(String username) {
        List<Exam> exams = new ArrayList<>();

        try {
            CollectionReference collectionReference = db.collection("exams");
            ApiFuture<QuerySnapshot> future = collectionReference
                    .whereEqualTo("username", username)
                    .get();

            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (DocumentSnapshot doc : documents) {
                if (doc.exists() && doc.contains("date")) { // Bỏ qua nếu thiếu date
                    Exam exam = doc.toObject(Exam.class);
                    if (exam.isActive()) {
                        exam.setExamID(doc.getId());
                        exams.add(exam);
                    }
                }
            }

            // Sắp xếp thủ công theo ngày giảm dần
            exams.sort((e1, e2) -> e2.getDate().compareTo(e1.getDate()));

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
            if (exam.isActive()){
            String normalizedExamName = normalizeString(exam.getExamName()); // Chuẩn hóa tên đề thi

            // Kiểm tra xem chuỗi tìm kiếm có nằm trong tên đề thi hay không
            if (normalizedExamName.contains(normalizedSearchTerm)) {
                exam.setExamID(doc.getId());

                if (exam.getDate() != null) {
                    String formattedDate = formatDate(exam.getDate());
                    exam.setDate(formattedDate);
                }

                exams.add(exam); // Thêm đề thi vào danh sách kết quả
            }
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
                if (exam.isActive()){
                    exam.setExamID(doc.getId());
                    if (exam.getDate() != null) {
                        String formattedDate = formatDate(exam.getDate());
                        exam.setDate(formattedDate);
                    }
                    resultList.add(exam);
                }


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
                if (exam.isActive())
                {
                exam.setExamID(doc.getId()); // Thêm dòng này để thiết lập ID cho đề thi
                examList.add(exam);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return examList;
    }
    // Xoá đề thi
    public void deleteExam(String examId) {
        DocumentReference examRef = db.collection("exams").document(examId);
        try {
            examRef.update("active", false).get();
            System.out.println("Cập nhật trạng thái active của exam " + examId + " thành " + false);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Lỗi cập nhật trạng thái của exam " + examId);
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

    public List<ExamResult> getExamResultsByExamId(String examId) {
        List<ExamResult> results = new ArrayList<>();
        try {
            System.out.println("Bắt đầu truy vấn examResults với examId: " + examId);
            CollectionReference collectionReference = db.collection("examResults");
            ApiFuture<QuerySnapshot> future = collectionReference
                    .whereEqualTo("examID", examId)
                    .get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            System.out.println("Số lượng tài liệu tìm thấy: " + documents.size());
            for (QueryDocumentSnapshot doc : documents) {
                System.out.println("Dữ liệu gốc từ Firestore: " + doc.getData());
                ExamResult examResult = doc.toObject(ExamResult.class);
                String[] parts = doc.getId().split("_");
                if (parts.length > 0) {
                    examResult.setUsername(parts[0]);
                }
                Map<String, List<String>> answers = new HashMap<>();
                doc.getData().forEach((key, value) -> {
                    if (key.startsWith("q") && value instanceof String) {
                        List<String> answerList = new ArrayList<>();
                        answerList.add((String) value);
                        answers.put(key, answerList);
                    }
                });
                examResult.setAnswers(answers);

                System.out.println("Kết quả sau ánh xạ: resultId=" + examResult.getResultId() +
                        ", username=" + examResult.getUsername() +
                        ", score=" + examResult.getScore() +
                        ", submittedAt=" + examResult.getSubmittedAt());
                results.add(examResult);
            }
            results.sort((r1, r2) -> Long.compare(r2.getSubmittedAt(), r1.getSubmittedAt()));
        } catch (Exception e) {
            System.err.println("Lỗi khi truy vấn examResults với examId " + examId + ": " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    public ExamStatistics calculateStatistics(List<ExamResult> results) {
        if (results == null || results.isEmpty()) {
            System.out.println("Danh sách kết quả rỗng!");
            return new ExamStatistics(0, 0, 0, 0, new LinkedHashMap<>());
        }

        int count = results.size();
        double sum = 0;
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        Map<String, Integer> distribution = new LinkedHashMap<>();

        String[] bins = {"0-2", "2-4", "4-6", "6-8", "8-10"};
        for (String bin : bins) {
            distribution.put(bin, 0);
        }

        for (ExamResult result : results) {
            double score = result.getScore();
            System.out.println("Điểm của kết quả: " + score);
            sum += score;
            min = Math.min(min, score);
            max = Math.max(max, score);

            if (score >= 0 && score < 2) distribution.put("0-2", distribution.get("0-2") + 1);
            else if (score >= 2 && score < 4) distribution.put("2-4", distribution.get("2-4") + 1);
            else if (score >= 4 && score < 6) distribution.put("4-6", distribution.get("4-6") + 1);
            else if (score >= 6 && score < 8) distribution.put("6-8", distribution.get("6-8") + 1);
            else if (score >= 8 && score <= 10) distribution.put("8-10", distribution.get("8-10") + 1);
        }

        double average = sum / count;
        System.out.println("Phân phối điểm: " + distribution);
        return new ExamStatistics(count, average, min, max, distribution);
    }

    // Lọc danh sách bài kiểm tra theo môn học
    public List<Exam> filterExamsBySubject(List<Exam> exams, String subject) {
        if (subject == null || subject.isEmpty() || subject.equals("all")) {
            return exams;
        }
        return exams.stream()
                .filter(exam -> exam.getSubject() != null && exam.getSubject().equalsIgnoreCase(subject))
                .collect(Collectors.toList());
    }

    // Lọc danh sách bài kiểm tra theo grade
    public List<Exam> filterExamsByClass(List<Exam> exams, String grade) { // Thay classLevel bằng grade
        if (grade == null || grade.isEmpty() || grade.equals("all")) {
            return exams;
        }
        return exams.stream()
                .filter(exam -> exam.getGrade() != null && exam.getGrade().equalsIgnoreCase(grade)) // Thay classLevel bằng grade
                .collect(Collectors.toList());
    }
}
