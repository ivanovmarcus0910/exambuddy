package com.example.exambuddy.model;

import java.util.List;

public class Question {
    private String questionText; // Câu hỏi
    private List<String> options; // Các lựa chọn
    private List<Integer> correctAnswers; // Nhiều đáp án đúng

    // Constructor mặc định
    public Question() {}

    // Getter và Setter
    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public List<String> getOptions() {
        return options;
    }

    public void setOptions(List<String> options) {
        this.options = options;
    }

    public List<Integer> getCorrectAnswers() {
        return correctAnswers;
    }

    public void setCorrectAnswers(List<Integer> correctAnswers) {
        this.correctAnswers = correctAnswers;
    }
}
