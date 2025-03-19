package com.example.exambuddy.service;

import com.example.exambuddy.config.FirebaseConfig;
import com.example.exambuddy.model.Question;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Service
public class QuestionBankService {

    @Autowired
    private FirebaseConfig firebaseConfig;

    private Firestore getFirestore() {
        return firebaseConfig.getFirestore();
    }

    // Lấy câu hỏi từ kho shared (không thay đổi)
    public List<Question> getSharedQuestions(String bookType, String classGrade, String subjectName,
                                             String chapterName, String lessonName, int page, int limit)
            throws ExecutionException, InterruptedException {
        Query query = getFirestore().collection("questionBank");
        if (!"all".equals(bookType)) query = query.whereEqualTo("bookType", bookType);
        if (!"all".equals(classGrade)) query = query.whereEqualTo("classGrade", classGrade);
        if (!"all".equals(subjectName)) query = query.whereEqualTo("subjectName", subjectName);
        if (!"all".equals(chapterName)) query = query.whereEqualTo("chapterName", chapterName);
        if (!"all".equals(lessonName)) query = query.whereEqualTo("lessonName", lessonName);
        query = query.offset((page - 1) * limit).limit(limit);

        return query.get().get().getDocuments().stream()
                .map(doc -> {
                    Question q = doc.toObject(Question.class);
                    q.setQuestionId(doc.getId());
                    return q;
                })
                .collect(Collectors.toList());
    }

    // Lấy câu hỏi từ kho private (không thay đổi)
    public List<Question> getPrivateQuestions(String userId, String bookType, String classGrade,
                                              String subjectName, String chapterName, String lessonName, int page, int limit)
            throws ExecutionException, InterruptedException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null!");
        }
        Query query = getFirestore().collection("users").document(userId).collection("questionBank");
        if (!"all".equals(bookType)) query = query.whereEqualTo("bookType", bookType);
        if (!"all".equals(classGrade)) query = query.whereEqualTo("classGrade", classGrade);
        if (!"all".equals(subjectName)) query = query.whereEqualTo("subjectName", subjectName);
        if (!"all".equals(chapterName)) query = query.whereEqualTo("chapterName", chapterName);
        if (!"all".equals(lessonName)) query = query.whereEqualTo("lessonName", lessonName);
        query = query.offset((page - 1) * limit).limit(limit);

        return query.get().get().getDocuments().stream()
                .map(doc -> {
                    Question q = doc.toObject(Question.class);
                    q.setQuestionId(doc.getId());
                    return q;
                })
                .collect(Collectors.toList());
    }

    // Thêm câu hỏi vào kho shared (cập nhật createdBy)
    public void addSharedQuestion(String bookType, String classGrade, String subjectName,
                                  String chapterName, String lessonName, Question question)
            throws ExecutionException, InterruptedException {
        if (question.getQuestionId() == null) {
            question.setQuestionId(UUID.randomUUID().toString());
        }
        if (question.getCreatedAt() == null) {
            question.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        Map<String, Object> questionData = questionToMap(bookType, classGrade, subjectName, chapterName, lessonName, question);
        getFirestore().collection("questionBank").document(question.getQuestionId()).set(questionData).get();
    }

    // Thêm câu hỏi vào kho private (cập nhật createdBy)
    public void addPrivateQuestion(String userId, String bookType, String classGrade,
                                   String subjectName, String chapterName, String lessonName,
                                   Question question) throws ExecutionException, InterruptedException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null!");
        }
        if (question.getQuestionId() == null) {
            question.setQuestionId(UUID.randomUUID().toString());
        }
        if (question.getCreatedAt() == null) {
            question.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        Map<String, Object> questionData = questionToMap(bookType, classGrade, subjectName, chapterName, lessonName, question);
        getFirestore().collection("users").document(userId)
                .collection("questionBank")
                .document(question.getQuestionId())
                .set(questionData).get();
    }

    // Đẩy câu hỏi từ private lên shared (phương thức mới)
    public void moveToShared(String userId, String bookType, String classGrade, String subjectName,
                             String chapterName, String lessonName, List<String> questionIds)
            throws ExecutionException, InterruptedException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null!");
        }
        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách questionIds không được rỗng!");
        }

        Firestore firestore = getFirestore();
        List<CompletableFuture<DocumentSnapshot>> readFutures = questionIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return firestore.collection("users").document(userId)
                                .collection("questionBank").document(id).get().get();
                    } catch (Exception e) {
                        System.err.println("Lỗi khi đọc câu hỏi " + id + ": " + e.getMessage());
                        return null;
                    }
                }, Executors.newFixedThreadPool(Math.min(questionIds.size(), 4))))
                .collect(Collectors.toList());

        List<Question> questionsToMove = CompletableFuture.allOf(readFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> readFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .filter(DocumentSnapshot::exists)
                        .map(doc -> doc.toObject(Question.class))
                        .collect(Collectors.toList()))
                .get();

        WriteBatch batch = firestore.batch();
        for (Question question : questionsToMove) {
            String newQuestionId = UUID.randomUUID().toString();
            question.setQuestionId(newQuestionId);
            question.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            Map<String, Object> questionData = questionToMap(bookType, classGrade, subjectName, chapterName, lessonName, question);
            DocumentReference newDocRef = firestore.collection("questionBank").document(newQuestionId);
            batch.set(newDocRef, questionData);
        }
        batch.commit().get();
    }

    // Lấy câu hỏi từ shared về private (không thay đổi)
    public void moveToPrivate(String userId, String bookType, String classGrade, String subjectName,
                              String chapterName, String lessonName, List<String> questionIds)
            throws ExecutionException, InterruptedException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null!");
        }
        if (questionIds == null || questionIds.isEmpty()) {
            throw new IllegalArgumentException("Danh sách questionIds không được rỗng!");
        }

        Firestore firestore = getFirestore();
        List<CompletableFuture<DocumentSnapshot>> readFutures = questionIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return firestore.collection("questionBank").document(id).get().get();
                    } catch (Exception e) {
                        System.err.println("Lỗi khi đọc câu hỏi " + id + ": " + e.getMessage());
                        return null;
                    }
                }, Executors.newFixedThreadPool(Math.min(questionIds.size(), 4))))
                .collect(Collectors.toList());

        List<Question> questionsToMove = CompletableFuture.allOf(readFutures.toArray(new CompletableFuture[0]))
                .thenApply(v -> readFutures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .filter(DocumentSnapshot::exists)
                        .map(doc -> doc.toObject(Question.class))
                        .collect(Collectors.toList()))
                .get();

        WriteBatch batch = firestore.batch();
        for (Question question : questionsToMove) {
            String newQuestionId = UUID.randomUUID().toString();
            question.setCreatedAt(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            question.setQuestionId(newQuestionId);
            Map<String, Object> questionData = questionToMap(bookType, classGrade, subjectName, chapterName, lessonName, question);
            DocumentReference newDocRef = firestore.collection("users").document(userId)
                    .collection("questionBank").document(newQuestionId);
            batch.set(newDocRef, questionData);
        }
        batch.commit().get();
    }

    // Xóa câu hỏi từ kho private (không thay đổi)
    public void deletePrivateQuestion(String userId, String questionId)
            throws ExecutionException, InterruptedException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null!");
        }
        if (questionId == null || questionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Question ID không được null hoặc rỗng!");
        }
        DocumentReference ref = getFirestore().collection("users").document(userId)
                .collection("questionBank").document(questionId);
        if (!ref.get().get().exists()) {
            throw new IllegalArgumentException("Câu hỏi không tồn tại trong kho cá nhân!");
        }
        ref.delete().get();
    }

    // Cập nhật câu hỏi trong kho private (không thay đổi)
    public void updatePrivateQuestionContent(String userId, String questionId, String questionText)
            throws ExecutionException, InterruptedException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null!");
        }
        if (questionId == null || questionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Question ID không được null hoặc rỗng!");
        }
        if (questionText == null || questionText.trim().isEmpty()) {
            throw new IllegalArgumentException("Nội dung câu hỏi không được null hoặc rỗng!");
        }
        DocumentReference ref = getFirestore().collection("users").document(userId)
                .collection("questionBank").document(questionId);
        if (!ref.get().get().exists()) {
            throw new IllegalArgumentException("Câu hỏi không tồn tại trong kho cá nhân!");
        }
        ref.update("questionText", questionText).get();
    }

    // Tạo đề thi (không thay đổi)
    public List<Question> generateExam(String userId, String repoType, int numQuestions, Map<String, Float> difficultyDistribution)
            throws ExecutionException, InterruptedException {
        if (userId == null) {
            throw new IllegalArgumentException("User ID không được null!");
        }
        List<Question> allQuestions = repoType.equals("shared")
                ? getSharedQuestions("all", "all", "all", "all", "all", 1, Integer.MAX_VALUE)
                : getPrivateQuestions(userId, "all", "all", "all", "all", "all", 1, Integer.MAX_VALUE);

        if (allQuestions.size() < numQuestions) {
            throw new IllegalArgumentException("Không đủ câu hỏi để tạo đề! Số câu hỏi hiện có: " + allQuestions.size());
        }

        Map<String, List<Question>> questionsByDifficulty = allQuestions.stream()
                .collect(Collectors.groupingBy(Question::getDifficulty));

        List<Question> examQuestions = new ArrayList<>();
        for (Map.Entry<String, Float> entry : difficultyDistribution.entrySet()) {
            String difficulty = entry.getKey();
            float proportion = entry.getValue();
            int count = Math.round(numQuestions * proportion);

            List<Question> available = questionsByDifficulty.getOrDefault(difficulty, new ArrayList<>());
            if (available.size() < count) {
                throw new IllegalArgumentException("Không đủ câu hỏi độ khó " + difficulty + ". Cần: " + count + ", Có: " + available.size());
            }

            Collections.shuffle(available);
            examQuestions.addAll(available.subList(0, count));
        }

        if (examQuestions.size() < numQuestions) {
            List<Question> remaining = allQuestions.stream()
                    .filter(q -> !examQuestions.contains(q))
                    .collect(Collectors.toList());
            Collections.shuffle(remaining);
            examQuestions.addAll(remaining.subList(0, numQuestions - examQuestions.size()));
        }

        return examQuestions;
    }

    // Chuyển dữ liệu Question thành Map (cập nhật để thêm createdBy)
    private Map<String, Object> questionToMap(String bookType, String classGrade, String subjectName,
                                              String chapterName, String lessonName, Question question) {
        Map<String, Object> data = new HashMap<>();
        data.put("bookType", question.getBookType() != null ? question.getBookType() : bookType);
        data.put("classGrade", question.getClassGrade() != null ? question.getClassGrade() : classGrade);
        data.put("subjectName", question.getSubjectName() != null ? question.getSubjectName() : subjectName);
        data.put("chapterName", question.getChapterName() != null ? question.getChapterName() : chapterName);
        data.put("lessonName", question.getLessonName() != null ? question.getLessonName() : lessonName);
        data.put("questionText", question.getQuestionText());
        data.put("options", question.getOptions());
        data.put("correctAnswers", question.getCorrectAnswers());
        data.put("difficulty", question.getDifficulty());
        data.put("createdAt", question.getCreatedAt());
        data.put("createdBy", question.getCreatedBy()); // Thêm thông tin người tạo
        return data;
    }
}