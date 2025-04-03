package com.example.exambuddy.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AdminLog {
    private String id;
    private String adminUsername;
    private String action;
    private String targetType;
    private String targetId;
    private String targetName;
    private String description;
    private LocalDateTime timestamp;
    private String targetDetails; // Trường mới để lưu nội dung post hoặc tên exam

    // Constructor
    public AdminLog(String adminUsername, String action, String targetType, String targetId, String targetName, String description) {
        this.adminUsername = adminUsername;
        this.action = action;
        this.targetType = targetType;
        this.targetId = targetId;
        this.targetName = targetName;
        this.description = description;
        this.timestamp = LocalDateTime.now();
    }

    // Getters và Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getAdminUsername() { return adminUsername; }
    public void setAdminUsername(String adminUsername) { this.adminUsername = adminUsername; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getTargetType() { return targetType; }
    public void setTargetType(String targetType) { this.targetType = targetType; }
    public String getTargetId() { return targetId; }
    public void setTargetId(String targetId) { this.targetId = targetId; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    public String getTargetDetails() { return targetDetails; }
    public void setTargetDetails(String targetDetails) { this.targetDetails = targetDetails; }

    // Phương thức xác định class
    public String getActionClass() {
        if (action == null) {
            return "";
        }
        String actionUpper = action.toUpperCase();
        if (actionUpper.contains("ADD") || actionUpper.contains("APPROVE")) {
            return "action-create";
        } else if (actionUpper.contains("UPDATE") || actionUpper.contains("CHANGE")) {
            return "action-update";
        } else if (actionUpper.contains("DISABLE") || actionUpper.contains("REJECT")) {
            return "action-delete";
        }
        return "";
    }

    // Phương thức định dạng timestamp
    public String getFormattedTimestamp() {
        if (timestamp == null) {
            return "";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return timestamp.format(formatter);
    }
}