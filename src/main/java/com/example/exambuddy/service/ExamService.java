package com.example.exambuddy.service;


import com.example.exambuddy.model.Exam;
import com.example.exambuddy.model.ExamResult;
import com.example.exambuddy.model.ExamStatistics;
import com.example.exambuddy.model.Question;
import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutures;
import com.google.api.gax.rpc.FailedPreconditionException;
import com.google.cloud.firestore.*;
import com.google.common.collect.Table;
import com.google.firebase.cloud.FirestoreClient;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xwpf.usermodel.*;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.MultiValueMap;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

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

    private final PaginationService<ExamResult> paginationService;

    // Inject PaginationService qua constructor
    public ExamService(PaginationService<ExamResult> paginationService) {
        this.paginationService = paginationService;
    }
    public List<Exam> getExamList() {
        List<Exam> exams = new ArrayList<>();
        try {
            // Lấy toàn bộ danh sách exam, sắp xếp theo số lượng người tham gia giảm dần
            Query query = db.collection("exams")
                    .whereEqualTo("status", "APPROVED")
                    .orderBy("participantCount", Query.Direction.DESCENDING);
            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot doc : documents) {
                Exam exam = doc.toObject(Exam.class);
                exam.setExamID(doc.getId());
                exams.add(exam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return exams;
    }

    public Map<String, List<Exam>> getExamPopular() {
        Map<String, List<Exam>> result = new HashMap<>();
        try {
            // Lấy toàn bộ danh sách exam, sắp xếp theo số lượng người tham gia giảm dần
            Query query = db.collection("exams")
                    .whereEqualTo("status", "APPROVED")
                    .orderBy("participantCount", Query.Direction.DESCENDING);
            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            // Danh sách riêng cho từng lớp
            List<Exam> grade10Exams = new ArrayList<>();
            List<Exam> grade11Exams = new ArrayList<>();
            List<Exam> grade12Exams = new ArrayList<>();

            for (QueryDocumentSnapshot doc : documents) {
                Exam exam = doc.toObject(Exam.class);
                exam.setExamID(doc.getId());

                // Phân loại theo lớp và giới hạn 4 bài mỗi lớp
                switch (exam.getGrade()) {
                    case "10":
                        if (grade10Exams.size() < 4) grade10Exams.add(exam);
                        break;
                    case "11":
                        if (grade11Exams.size() < 4) grade11Exams.add(exam);
                        break;
                    case "12":
                        if (grade12Exams.size() < 4) grade12Exams.add(exam);
                        break;
                }
            }

            // Lưu kết quả vào Map
            result.put("grade10Exams", grade10Exams);
            result.put("grade11Exams", grade11Exams);
            result.put("grade12Exams", grade12Exams);

        } catch (ExecutionException e) {
            if (e.getCause() instanceof FailedPreconditionException) {
                System.err.println("Lỗi: Truy vấn yêu cầu một chỉ mục composite. Vui lòng tạo chỉ mục.");
                // Ở đây bạn có thể thêm logic để thông báo lỗi này cho người dùng hoặc admin
            } else {
                System.err.println("Lỗi không xác định khi lấy danh sách đề thi phổ biến: " + e.getMessage());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println("Thread bị gián đoạn khi lấy danh sách đề thi phổ biến: " + e.getMessage());
        }
        return result;
    }


    // Phương thức format lại ngày
    public static String formatDate(String dateString) {
        try {
            // Chuẩn hóa chuỗi đầu vào (cắt bớt micro giây và thêm 'Z' nếu cần)
            String normalizedDate = dateString.replaceAll("\\.(\\d{3})\\d*", ".$1"); // Giữ 3 chữ số mili giây
            if (!normalizedDate.endsWith("Z")) {
                normalizedDate += "Z"; // Thêm 'Z' nếu thiếu
            }

            // Định dạng đầu vào từ Firestore (ISO 8601)
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            originalFormat.setTimeZone(TimeZone.getTimeZone("UTC")); // Chuỗi gốc là UTC
            Date date = originalFormat.parse(normalizedDate);

            // Định dạng đầu ra mới: "dd/MM/yyyy HH:mm:ss" (24 giờ)
            SimpleDateFormat targetFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            targetFormat.setTimeZone(TimeZone.getDefault()); // Chuyển sang múi giờ hệ thống
            return targetFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi định dạng ngày: " + e.getMessage();
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
            // Đặt trạng thái active là false để yêu cầu duyệt của admin
            examDataMap.put("status", "PENDING");

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
            Sheet sheet = workbook.getSheetAt(0);


            List<Map<String, Object>> questions = new ArrayList<>();


            for (int i = 0; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;


                int lastCellNum = row.getLastCellNum(); // tổng số cột
                if (lastCellNum < 3) continue; // cần ít nhất 1 câu hỏi + 1 đáp án + 1 đáp án đúng


                Map<String, Object> questionData = new HashMap<>();
                questionData.put("questionText", row.getCell(0).getStringCellValue());


                List<String> options = new ArrayList<>();
                for (int j = 1; j < lastCellNum - 1; j++) {
                    Cell cell = row.getCell(j);
                    options.add(cell != null ? cell.getStringCellValue() : "");
                }
                questionData.put("options", options);


                // Cột cuối cùng là đáp án đúng
                Cell answerCell = row.getCell(lastCellNum - 1);
                String correctAnswerStr = answerCell != null ? answerCell.getStringCellValue().toUpperCase() : "";
                List<Integer> correctAnswers = new ArrayList<>();
                for (String ans : correctAnswerStr.split(",")) {
                    int index = ans.trim().charAt(0) - 'A'; // A=0, B=1, ...
                    if (index >= 0 && index < options.size()) {
                        correctAnswers.add(index);
                    } else {
                        throw new IllegalArgumentException("Đáp án không hợp lệ: " + ans);
                    }
                }
                questionData.put("correctAnswers", correctAnswers);


                questions.add(questionData);
            }


            examData.put("questions", questions);
            examData.put("date", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date()));


            return addExam(examData, "");


        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public List<Map<String, Object>> extractQuestionsFromDocx(InputStream inputStream) {
        List<Map<String, Object>> questions = new ArrayList<>();
        try {
            XWPFDocument doc = new XWPFDocument(inputStream);
            StringBuilder text = new StringBuilder();


            // Đọc toàn bộ đoạn văn bản
            for (XWPFParagraph para : doc.getParagraphs()) {
                String line = para.getText().trim();
                if (!line.isEmpty()) {
                    text.append(line).append("\n");
                }
            }


            String questionsText = text.toString().split("-----------HẾT----------")[0].trim();


            // Regex tìm từng câu hỏi cùng block đáp án
            Pattern questionPattern = Pattern.compile(
                    "Câu \\d+:(.*?)((?:\\s*[A-F]\\..*?)+)(?=\\s*Câu \\d+:|$)",
                    Pattern.DOTALL
            );
            Matcher questionMatcher = questionPattern.matcher(questionsText);


            while (questionMatcher.find()) {
                Map<String, Object> questionData = new HashMap<>();


                String questionText = questionMatcher.group(1).trim();
                String optionsBlock = questionMatcher.group(2).trim();


                // Regex tách các đáp án A. đến F.
                Pattern optionPattern = Pattern.compile("([A-F])\\.\\s*((?:.(?![A-F]\\.))+)", Pattern.DOTALL);
                Matcher optionMatcher = optionPattern.matcher(optionsBlock);


                List<String> options = new ArrayList<>();
                while (optionMatcher.find() && options.size() < 6) {
                    options.add(optionMatcher.group(2).trim());
                }


                questionData.put("questionText", questionText);
                questionData.put("options", options);
                questionData.put("correctAnswers", new ArrayList<>());
                questions.add(questionData);
            }


            // Tìm bảng đáp án
            XWPFTable answerTable = null;
            for (XWPFTable table : doc.getTables()) {
                for (XWPFTableRow row : table.getRows()) {
                    for (XWPFTableCell cell : row.getTableCells()) {
                        String cellText = cell.getText().trim();
                        if (cellText.contains("ĐÁP ÁN") || cellText.matches(".*\\d+\\s*[.\\s]\\s*[A-F].*")) {
                            answerTable = table;
                            break;
                        }
                    }
                    if (answerTable != null) break;
                }
                if (answerTable != null) break;
            }


            if (answerTable == null) throw new Exception("Không tìm thấy bảng đáp án trong file.");


            Map<Integer, String> answerMap = new HashMap<>();
            for (XWPFTableRow row : answerTable.getRows()) {
                for (XWPFTableCell cell : row.getTableCells()) {
                    String cellText = cell.getText().trim();
                    Pattern answerPattern = Pattern.compile("(\\d+)[.\\s]*([A-F])");
                    Matcher matcher = answerPattern.matcher(cellText);
                    while (matcher.find()) {
                        int qIndex = Integer.parseInt(matcher.group(1)) - 1;
                        answerMap.put(qIndex, matcher.group(2));
                    }
                }
            }


            for (int i = 0; i < questions.size(); i++) {
                if (answerMap.containsKey(i)) {
                    String correct = answerMap.get(i);
                    List<Integer> correctList = new ArrayList<>();
                    switch (correct) {
                        case "A" -> correctList.add(0);
                        case "B" -> correctList.add(1);
                        case "C" -> correctList.add(2);
                        case "D" -> correctList.add(3);
                        case "E" -> correctList.add(4);
                        case "F" -> correctList.add(5);
                    }
                    questions.get(i).put("correctAnswers", correctList);
                }
            }


        } catch (Exception e) {
            e.printStackTrace();
        }
        return questions;
    }


    public Map<String, Integer> getAvailableChapters(String username, String subject, String grade)
            throws ExecutionException, InterruptedException {


        Query query = db.collection("users").document(username)
                .collection("questionBank")
                .whereEqualTo("subjectName", subject)
                .whereEqualTo("classGrade", grade);


        // Nhóm theo chapterName và đếm số câu hỏi
        return query.get().get().getDocuments().stream()
                .collect(Collectors.groupingBy(
                        doc -> doc.getString("chapterName"),
                        Collectors.summingInt(doc -> 1)
                ));
    }
    public List<Question> generateQuestionsFromPool(List<Question> pool, Map<String, Integer> config) {
        List<Question> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : config.entrySet()) {
            String chapter = entry.getKey();
            int count = entry.getValue();
            List<Question> chapterQuestions = pool.stream()
                    .filter(q -> chapter.equals(q.getChapterName()))
                    .collect(Collectors.toList());
            Collections.shuffle(chapterQuestions);
            result.addAll(chapterQuestions.stream().limit(count).collect(Collectors.toList()));
        }
        return result;
    }
    public Exam createExamFromBank(
            String examName, String grade, String subject,
            String examType, String city, int timeduration,
            Map<String, Integer> chapterConfig, String username
    ) throws ExecutionException, InterruptedException {


        // 1. Lấy toàn bộ câu hỏi từ các chương được chọn
        List<Question> questionPool = new ArrayList<>();
        for (String chapterName : chapterConfig.keySet()) {
            questionPool.addAll(getQuestionsByChapter(username, subject, grade, chapterName));
        }


        // 2. Chọn ngẫu nhiên câu hỏi theo config
        List<Question> displayedQuestions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : chapterConfig.entrySet()) {
            String chapterName = entry.getKey();
            int requiredCount = entry.getValue();


            List<Question> chapterQuestions = questionPool.stream()
                    .filter(q -> q.getChapterName().equals(chapterName))
                    .collect(Collectors.toList());


            Collections.shuffle(chapterQuestions);
            displayedQuestions.addAll(chapterQuestions.stream()
                    .limit(requiredCount)
                    .collect(Collectors.toList()));
        }


        // 3. Tạo đối tượng Exam và gán thông tin
        Exam exam = new Exam();
        exam.setFromQuestionBank(true); // 🟢 Đặt dòng này ở đây


        String examId = UUID.randomUUID().toString();


        exam.setExamID(examId);
        exam.setExamName(examName);
        exam.setGrade(grade);
        exam.setSubject(subject);
        exam.setExamType(examType);
        exam.setCity(city);
        exam.setTimeduration(timeduration);
        exam.setUsername(username);
        exam.setQuestions(displayedQuestions);
        exam.setQuestionPool(questionPool); // Lưu toàn bộ pool
        exam.setChapterConfig(chapterConfig);

        exam.setParticipantCount(0);
        exam.setDate(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date()));


        // 4. Chuyển exam thành Map và lưu vào Firestore
        Map<String, Object> examData = new HashMap<>();
        examData.put("examID", exam.getExamID());
        examData.put("examName", exam.getExamName());
        examData.put("grade", exam.getGrade());
        examData.put("subject", exam.getSubject());
        examData.put("examType", exam.getExamType());
        examData.put("city", exam.getCity());
        examData.put("timeduration", exam.getTimeduration());
        examData.put("username", exam.getUsername());
        examData.put("questions", exam.getQuestions()); // Có thể bỏ nếu chỉ lưu trong subcollection
        examData.put("questionPool", exam.getQuestionPool());
        examData.put("chapterConfig", exam.getChapterConfig());

        examData.put("participantCount", exam.getParticipantCount());
        examData.put("date", exam.getDate());
        examData.put("fromQuestionBank", true);


        // 5. Lưu exam vào Firestore
        DocumentReference examRef = db.collection("exams").document(examId);
        examRef.set(examData).get();


        // 6. Lưu các câu hỏi hiển thị vào subcollection "questions"
        WriteBatch batch = db.batch();
        for (Question q : displayedQuestions) {
            String questionId = UUID.randomUUID().toString();
            DocumentReference qRef = examRef.collection("questions").document(questionId);
            batch.set(qRef, q); // Question là POJO
        }
        batch.commit().get();


        return exam;
    }


    private List<Question> getQuestionsByChapter(
            String username, String subject, String grade, String chapterName
    ) throws ExecutionException, InterruptedException {


        Query query = db.collection("users").document(username)
                .collection("questionBank")
                .whereEqualTo("subjectName", subject)
                .whereEqualTo("classGrade", grade)
                .whereEqualTo("chapterName", chapterName);


        return query.get().get().getDocuments().stream()
                .map(doc -> {
                    Question q = doc.toObject(Question.class);
                    q.setQuestionId(doc.getId());
                    return q;
                })
                .collect(Collectors.toList());
    }
    public List<Question> generateExamQuestions(String examId) throws ExecutionException, InterruptedException {
        DocumentSnapshot examDoc = db.collection("exams").document(examId).get().get();


        // Lấy questionPool
        Object questionPoolObj = examDoc.get("questionPool");
        if (!(questionPoolObj instanceof List)) {
            throw new IllegalStateException("questionPool phải là một List");
        }


        List<?> rawQuestionPool = (List<?>) questionPoolObj;
        List<Map<String, Object>> questionPool = new ArrayList<>();
        for (Object item : rawQuestionPool) {
            if (!(item instanceof Map)) {
                throw new IllegalStateException("Mỗi phần tử trong questionPool phải là một Map");
            }
            questionPool.add((Map<String, Object>) item);
        }


        // Lấy chapterConfig
        Object chapterConfigObj = examDoc.get("chapterConfig");
        if (!(chapterConfigObj instanceof Map)) {
            throw new IllegalStateException("chapterConfig phải là một Map");
        }


        Map<?, ?> rawChapterConfig = (Map<?, ?>) chapterConfigObj;
        Map<String, Integer> chapterConfig = new HashMap<>();
        for (Map.Entry<?, ?> entry : rawChapterConfig.entrySet()) {
            if (!(entry.getKey() instanceof String) || !(entry.getValue() instanceof Integer)) {
                throw new IllegalStateException("chapterConfig phải có key là String và value là Integer");
            }
            chapterConfig.put((String) entry.getKey(), (Integer) entry.getValue());
        }


        // Tạo danh sách câu hỏi ngẫu nhiên
        List<Question> questions = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : chapterConfig.entrySet()) {
            String chapterId = entry.getKey();
            int count = entry.getValue();


            List<Question> chapterQuestions = questionPool.stream()
                    .filter(q -> chapterId.equals(q.get("chapterId")))
                    .map(q -> {
                        Question question = new Question();
                        question.setQuestionText((String) q.get("questionText"));
                        question.setChapterId((String) q.get("chapterId"));
                        // Thêm các trường khác nếu cần
                        return question;
                    })
                    .collect(Collectors.toList());


            Collections.shuffle(chapterQuestions);
            questions.addAll(chapterQuestions.stream().limit(count).collect(Collectors.toList()));
        }


        return questions;
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

    public Page<ExamResult> getExamResultByUsername(String userId, Pageable pageable) {
        List<ExamResult> results = new ArrayList<>();
        long totalItems = 0;

        try {
            Firestore db = FirestoreClient.getFirestore();
            CollectionReference collectionReference = db.collection("examResults");

            // Lọc ngay trong query để lấy đúng dữ liệu của user
            Query query = collectionReference
                    .whereGreaterThanOrEqualTo(FieldPath.documentId(), userId + "_")  // Chỉ lấy tài liệu có userId đầu tiên
                    .whereLessThan(FieldPath.documentId(), userId + "_\uf8ff")       // Giới hạn trong phạm vi userId
                    .orderBy("submittedAt", Query.Direction.DESCENDING);             // Sắp xếp giảm dần

            // Lấy tổng số bài thi của user
            ApiFuture<QuerySnapshot> countFuture = query.get();
            List<QueryDocumentSnapshot> allDocs = countFuture.get().getDocuments();
            totalItems = allDocs.size();

            // Áp dụng phân trang
            query = query.limit(pageable.getPageSize())
                    .offset((int) pageable.getOffset());  // Bỏ qua số lượng cần thiết

            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            results = documents.stream()
                    .map(doc -> doc.toObject(ExamResult.class))
                    .collect(Collectors.toList());

        } catch (Exception e) {
            System.err.println("Lỗi khi lấy kết quả bài thi cho userId: " + userId + " - " + e.getMessage());
        }

        return new PageImpl<>(results, pageable, totalItems);
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
            String status = (String) examData.get("status");

            // 3. Tạo map dữ liệu để lưu vào likedExams
            Map<String, Object> likeData = new HashMap<>();
            likeData.put("username", userId);
            likeData.put("examID", examId);
            likeData.put("examName", examName);
            likeData.put("subject", subject);
            likeData.put("grade", grade);
            likeData.put("date", createdDate);
            likeData.put("status", status);

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
                if (exam.getStatus().equals("APPROVED")) {
                    exam.setExamID(doc.getString("examID"));
                    likedExams.add(exam);
                }
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

    public Page<Exam> getHistoryCreateExamsByUsername(String username, Pageable pageable) {
        List<Exam> exams;
        long totalItems = 0;

        try {
            CollectionReference collectionReference = db.collection("exams");

            // Truy vấn chỉ lấy dữ liệu của user
            Query query = collectionReference
                    .whereEqualTo("username", username)
                    .orderBy("date", Query.Direction.DESCENDING); // Sắp xếp theo ngày giảm dần

            // Lấy tổng số bài thi của user
            ApiFuture<QuerySnapshot> countFuture = query.get();
            List<QueryDocumentSnapshot> allDocs = countFuture.get().getDocuments();
            totalItems = allDocs.size();

            // Áp dụng phân trang
            query = query.limit(pageable.getPageSize())
                    .offset((int) pageable.getOffset());

            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            exams = documents.stream()
                    .map(doc -> {
                        Exam exam = doc.toObject(Exam.class);
                        exam.setExamID(doc.getId()); // Gán ID từ Firestore
                        return exam;
                    })
                    .collect(Collectors.toList());

        } catch (InterruptedException | ExecutionException e) {
            System.err.println("Lỗi khi lấy danh sách bài kiểm tra của username: " + username + " - " + e.getMessage());
            return Page.empty();
        }

        return new PageImpl<>(exams, pageable, totalItems);
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

        ApiFuture<QuerySnapshot> querySnapshot = db.collection("exams")
                .whereEqualTo("status", "APPROVED")
                .get();
        List<QueryDocumentSnapshot> documents = querySnapshot.get().getDocuments();

        for (QueryDocumentSnapshot doc : documents) {
            Exam exam = doc.toObject(Exam.class);

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

        return exams; // Trả về danh sách đề thi phù hợp
    }

    public List<Exam> searchExamsByFilter(String grade, String subject, String examType, String city) {
        List<Exam> resultList = new ArrayList<>();
        Firestore db = FirestoreClient.getFirestore();
        try {
            CollectionReference examsRef = db.collection("exams");
            Query query = db.collection("exams")
                    .whereEqualTo("status", "APPROVED");;

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


            // Sắp xếp kết quả theo ngày (mới nhất trước)
            query = query.orderBy("date", Query.Direction.DESCENDING);
            // Thực hiện truy vấn
            ApiFuture<QuerySnapshot> future = query.get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            // Xử lý kết quả
            for (QueryDocumentSnapshot doc : documents) {
                Exam exam = doc.toObject(Exam.class);

                    exam.setExamID(doc.getId());
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
                exam.setExamID(doc.getId());
                examList.add(exam);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return examList;
    }

    // Thay thế phương thức deleteExam ban đầu bằng disableExam
    public void disableExam(String examId) {
        DocumentReference examRef = db.collection("exams").document(examId);
        try {
            // Cập nhật trường "status" thành "DISABLED"
            examRef.update("status", "DISABLED").get();
            System.out.println("Đã chuyển đề thi " + examId + " sang trạng thái DISABLED.");
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Lỗi khi chuyển trạng thái đề thi " + examId);
            e.printStackTrace();
        }
    }


    /**
     * Phương thức mới: cập nhật trạng thái active của Exam
     */
    public void updateExamStatus(String examId, String status) {
        DocumentReference examRef = db.collection("exams").document(examId);
        try {
            examRef.update("status", status).get();
            System.out.println("Cập nhật trạng thái của exam " + examId + " thành " + status);
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
