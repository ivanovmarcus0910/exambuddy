package com.example.exambuddy.model;

import org.springframework.context.annotation.Bean;

import java.util.List;
import java.util.Map;

public class ExamResult {
    private String resultId;
    private String examID;
    private double score;
    private Map<String, List<String>> answers;
    private long submittedAt;
    private List<String> correctAnswers;


    public ExamResult() {
    }

    public ExamResult(String resultId, String examID, double score, Map<String, List<String>> answers, long submittedAt, List<String> correctAnswers) {
        this.resultId = resultId;
        this.examID = examID;
        this.score = score;
        this.answers = answers;
        this.submittedAt = submittedAt;
        this.correctAnswers = correctAnswers;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public Map<String, List<String>> getAnswers() {
        return answers;
    }

    public void setAnswers(Map<String, List<String>> answers) {
        this.answers = answers;
    }

    public long getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(long submittedAt) {
        this.submittedAt = submittedAt;
    }

    public String getResultId() {
        return resultId;
    }

    public void setResultId(String resultId) {
        this.resultId = resultId;
    }

    public String getExamID() {
        return examID;
    }

    public void setExamID(String examID) {
        this.examID = examID;
    }

    public List<String> getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(List<String> correctAnswers) {
        this.correctAnswers = correctAnswers;
    }

}
