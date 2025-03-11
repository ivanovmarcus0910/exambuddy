package com.example.exambuddy.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Comment {
    private String commentId;
    private String postId;
    private String parentCommentId;
    private String content;
    private String username;
    private String date;
    private String avatarUrl;
    private List<String> imageUrls;
    private int likeCount;
    private List<String> likedUsernames;
    private boolean liked;
    private List<Comment> replies;
    private LocalDateTime localDateTime;
    private String postOwner;
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

    public String getParentCommentId() {
        return parentCommentId;
    }

    public void setParentCommentId(String parentCommentId) {
        this.parentCommentId = parentCommentId;
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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public void setTimeAgo(String timeAgo) {
    }

    public String getTimeAgo() {
        if (date == null || date.isEmpty()) return "Không xác định";

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime postTime = LocalDateTime.parse(date, formatter);
        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(postTime, now);

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
            return postTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        }
    }

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public List<String> getLikedUsernames() {
        return likedUsernames;
    }

    public void setLikedUsernames(List<String> likedUsernames) {
        this.likedUsernames = likedUsernames;
    }

    public boolean isLiked() {
        return liked;
    }

    public void setLiked(boolean liked) {
        this.liked = liked;
    }

    public List<Comment> getReplies() {
        return replies;
    }

    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }
    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public String getPostOwner() {
        return postOwner;
    }

    public void setPostOwner(String postOwner) {
        this.postOwner = postOwner;
    }
}
