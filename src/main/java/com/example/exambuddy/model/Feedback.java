package com.example.exambuddy.model;

import net.minidev.json.annotate.JsonIgnore;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class Feedback {
    private String feedbackId;
    private String examId;
    private String content;
    private String username;
    private String date;
    private String avatarUrl;
    private int rate;
    private String parentFeedbackId; // ID của feedback cha (nếu là reply)
    private boolean isReply;

    // Constructor không tham số
    public Feedback() {
    }

    // Constructor có tham số
    public Feedback(String feedbackId, String examId, String content, String username, String date, String avatarUrl, int rate, String parentFeedbackId, boolean isReply) {
        this.feedbackId = feedbackId;
        this.examId = examId;
        this.content = content;
        this.username = username;
        this.date = date;
        this.avatarUrl = avatarUrl;
        this.rate = rate;
    }

    // Getter và Setter
    public String getFeedbackId() {
        return feedbackId;
    }

    public void setFeedbackId(String feedbackId) {
        this.feedbackId = feedbackId;
    }

    public String getExamId() {
        return examId;
    }

    public void setExamId(String examId) {
        this.examId = examId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public int getRate() {
        return rate;
    }

    public void setRate(int rate) {
        this.rate = rate;
    }

    public String getParentFeedbackId() {
        return parentFeedbackId;
    }
    public void setParentFeedbackId(String parentFeedbackId) {
        this.parentFeedbackId = parentFeedbackId;
    }
    public boolean isReply() {
        return isReply;
    }
    public void setReply(boolean isReply) {
        this.isReply = isReply;
    }
    @JsonIgnore
    public String getTimeAgo() {
        if (date == null || date.isEmpty()) return "Không xác định";

        DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        try {
            LocalDateTime feedbackTime = LocalDateTime.parse(date, formatter);
            LocalDateTime now = LocalDateTime.now();
            Duration duration = Duration.between(feedbackTime, now);

            long seconds = duration.getSeconds();
            long minutes = seconds / 60;
            long hours = minutes / 60;

            if (seconds < 60) {
                return "Vừa mới đăng";
            } else if (minutes < 60) {
                return minutes + " phút trước";
            } else if (hours < 24) {
                return hours + " giờ trước";
            } else {
                return feedbackTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            }
        } catch (DateTimeParseException e) {
            return "Ngày không hợp lệ";
        }
    }

    // Xóa setter không cần thiết cho timeAgo vì nó là giá trị tính toán
    // public void setTimeAgo(String timeAgo) { }
}