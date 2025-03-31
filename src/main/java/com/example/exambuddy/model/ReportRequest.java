package com.example.exambuddy.model;

import java.util.Date;
import java.util.List;

public class ReportRequest {
    private String id;         // Trường lưu document ID của báo cáo
    private String postId;
    private List<String> reason;
    private String description;
    private String reporter;   // Người báo cáo
    private Date timestamp;    // Thời gian báo cáo
    private String postContent;
    private String postAuthor;
    private boolean postActive;

    public ReportRequest() {
    }

    // Getters và Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public List<String> getReason() {
        return reason;
    }

    public void setReasons(List<String> reason) {
        this.reason = reason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReporter() {
        return reporter;
    }

    public void setReporter(String reporter) {
        this.reporter = reporter;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }
    public String getPostContent() {
        return postContent;
    }
    public void setPostContent(String postContent) {
        this.postContent = postContent;
    }

    public String getPostAuthor() {
        return postAuthor;
    }
    public void setPostAuthor(String postAuthor) {
        this.postAuthor = postAuthor;
    }
    public boolean isPostActive() {
        return postActive;
    }

    public void setPostActive(boolean postActive) {
        this.postActive = postActive;
    }
}
