package com.example.exambuddy.model;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class Notification {
    private String notificationId;
    private String postId;
    private String sender;
    private String receiver;
    private String content;
    private Date date;
    private String type;
    private boolean isRead;

    public Notification() {
    }

    public Notification(String notificationId, String postId, String sender, String receiver, String content, Date date, String type, boolean isRead) {
        this.notificationId = notificationId;
        this.postId = postId;
        this.sender = sender;
        this.receiver = receiver;
        this.content = content;
        this.date = date;
        this.type = type;
        this.isRead = isRead;
    }

    public String getNotificationId() {
        return notificationId;
    }

    public void setNotificationId(String notificationId) {
        this.notificationId = notificationId;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getReceiver() {
        return receiver;
    }

    public void setReceiver(String receiver) {
        this.receiver = receiver;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public void setTimeAgo(String timeAgo) {
    }

    public String getTimeAgo() {
        if (date == null) return "Không xác định";

        // Chuyển từ Date sang LocalDateTime
        Instant instant = date.toInstant();
        LocalDateTime postTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime now = LocalDateTime.now();

        // Tính khoảng cách giữa hai thời điểm
        Duration duration = Duration.between(postTime, now);
        long seconds = duration.getSeconds();
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (seconds < 60) {
            return "Vừa mới đăng";
        } else if (minutes < 60) {
            return minutes + " phút trước";
        } else if (hours < 24) {
            return hours + " giờ trước";
        } else if (days < 7) {
            return days + " ngày trước";
        } else {
            return postTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }
}
