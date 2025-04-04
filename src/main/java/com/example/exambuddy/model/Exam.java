package com.example.exambuddy.model;

import com.google.cloud.firestore.annotation.IgnoreExtraProperties;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

@IgnoreExtraProperties
public class Exam {
    private String examID;
    private String examName;
    private String grade;
    private String subject;
    private String examType;
    private String city;
    private List<String> tags;
    private String username;
    private String date;
    private int questionCount; // Số lượng câu hỏi
    private List<Question> questions; // Danh sách câu hỏi
    private int participantCount;
    private List<Question> questionPool; // Toàn bộ câu hỏi gốc
    private Map<String, Integer> chapterConfig; // Ví dụ: {"Hàm số": 2, "Phương trình": 1}
    private boolean fromQuestionBank;
    private long timeduration;
    private String status; // Status dưới dạng String

    // Constructor mặc định: status mặc định là "PENDING"
    public Exam() {
        //this.status = "PENDING";
    }

    // Constructor có tham số
    public Exam(String examID, String examName, String grade, String subject, String examType, String city,
                List<String> tags, String username, String date, int questionCount, List<Question> questions,
                long timeduration, int participantCount ,String status) {
        this.examID = examID;
        this.examName = examName;
        this.grade = grade;
        this.subject = subject;
        this.examType = examType;
        this.city = city;
        this.tags = tags;
        this.username = username;
        this.date = date;
        this.questionCount = questionCount;
        this.questions = questions;
        this.timeduration = timeduration;
        this.participantCount = participantCount;
        this.status = status; // Mặc định là "PENDING"
    }

    // Getters và Setters
    public String getExamID() {
        return examID;
    }

    public void setExamID(String examID) {
        this.examID = examID;
    }

    public String getExamName() {
        return examName;
    }

    public void setExamName(String examName) {
        this.examName = examName;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getExamType() {
        return examType;
    }

    public void setExamType(String examType) {
        this.examType = examType;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public int getQuestionCount() {
        return questionCount;
    }

    public void setQuestionCount(int questionCount) {
        this.questionCount = questionCount;
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public void setQuestions(List<Question> questions) {
        this.questions = questions;
    }

    public long getTimeduration() {
        return timeduration;
    }

    public void setTimeduration(long timeduration) {
        this.timeduration = timeduration;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public List<Question> getQuestionPool() {
        return questionPool;
    }

    public void setQuestionPool(List<Question> questionPool) {
        this.questionPool = questionPool;
    }

    public Map<String, Integer> getChapterConfig() {
        return chapterConfig;
    }

    public void setChapterConfig(Map<String, Integer> chapterConfig) {
        this.chapterConfig = chapterConfig;
    }

    public boolean isFromQuestionBank() {
        return fromQuestionBank;
    }

    public void setFromQuestionBank(boolean fromQuestionBank) {
        this.fromQuestionBank = fromQuestionBank;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    // Phương thức định dạng ngày
    public String getFormattedDate() {
        return formatDate(this.date);
    }

    public static String formatDate(String dateString) {
        try {
            SimpleDateFormat targetFormat = new SimpleDateFormat("dd/MM/yyyy");

            // Nếu đầu vào đã là dd/MM/yyyy, không cần format lại
            if (dateString.matches("\\d{2}/\\d{2}/\\d{4}")) {
                return dateString;
            }

            // Xử lý nếu đầu vào ở dạng yyyy-MM-dd'T'HH:mm:ss.SSS'Z'
            SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            originalFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

            Date date = originalFormat.parse(dateString);
            return targetFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return "Lỗi định dạng ngày!";
        }
    }
}
