package com.example.exambuddy.model;

import java.util.List;

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

    // Constructors
    public Exam() {
    }

    public Exam(String examID, String examName, String grade, String subject, String examType, String city,
                List<String> tags, String username, String date, int questionCount, List<Question> questions) {
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
    }

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
}