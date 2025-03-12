package com.example.exambuddy.model;

import org.springframework.context.annotation.Bean;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class ExamResult {
    private String resultId;
    private String examID;
    private String examName;
    private double score;
    private Map<String, List<String>> answers;
    private long submittedAt;
    private List<String> correctAnswers;
    private String username;


    public ExamResult() {}

    public ExamResult(String resultId, String examID, String examName, double score,
                      Map<String, List<String>> answers, long submittedAt, List<String> correctAnswers, String username) {
        this.resultId = resultId;
        this.examID = examID;
        this.examName = examName;
        this.score = score;
        this.answers = answers;
        this.submittedAt = submittedAt;
        this.correctAnswers = correctAnswers;
        this.username = username;
    }

    // Getters và Setters
    public String getResultId() { return resultId; }
    public void setResultId(String resultId) { this.resultId = resultId; }

    public String getExamID() { return examID; }
    public void setExamID(String examID) { this.examID = examID; }

    public String getExamName() { return examName; }
    public void setExamName(String examName) { this.examName = examName; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public Map<String, List<String>> getAnswers() { return answers; }
    public void setAnswers(Map<String, List<String>> answers) { this.answers = answers; }

    public long getSubmittedAt() { return submittedAt; }
    public void setSubmittedAt(long submittedAt) { this.submittedAt = submittedAt; }

    public List<String> getCorrectAnswers() { return correctAnswers; }
    public void setCorrectAnswers(List<String> correctAnswers) { this.correctAnswers = correctAnswers; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getFormattedSubmittedAt() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        return sdf.format(new Date(submittedAt));
    }
}
