package com.example.exambuddy.model;

import java.util.List;

public class ReportRequest {
    private String postId;
    private List<String> reasons;
    private String description;
    private String reporter; // Người báo cáo
    private String avatarUrl;

    public ReportRequest() {
    }

    // Getter và Setter
    public String getPostId() { return postId; }
    public void setPostId(String postId) { this.postId = postId; }

    public List<String> getReasons() {
        return reasons;
    }

    public void setReasons(List<String> reasons) {
        this.reasons = reasons;
    }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avartaUrl) {
        this.avatarUrl = avartaUrl;
    }
}
