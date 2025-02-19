package com.example.exambuddy.model;
import java.util.List;

public class Comment {
    private String commentId;
    private String postId;
    private String content;
    private String username;
    private String date;
    private List<String> imageUrls;

    public Comment() {
    }

    public Comment(String commentId, String postId, String content, String username, String date, List<String> imageUrls) {
        this.commentId = commentId;
        this.postId = postId;
        this.content = content;
        this.username = username;
        this.date = date;
        this.imageUrls = imageUrls;
    }

    public String getCommentId() {
        return commentId;
    }

    public void setCommentId(String commentId) {
        this.commentId = commentId;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
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

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
