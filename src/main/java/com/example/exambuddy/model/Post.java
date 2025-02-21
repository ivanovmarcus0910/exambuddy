package com.example.exambuddy.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class Post {
    private String postId;
    private String username;
    private String content;
    private String date;
    private String avatarUrl;
    private List<String> imageUrls;
    private int likeCount;
    private List<Comment> comments;
    private List<String> likedUsernames;
    private boolean liked;

    public Post() {
    }

    public Post(String postId, String username, String content, String date, List<String> imageUrls, int likeCount, List<Comment> comments, List<String> likedUsernames) {
        this.postId = postId;
        this.username = username;
        this.content = content;
        this.date = date;
        this.imageUrls = imageUrls;
        this.likeCount = likeCount;
        this.comments = comments;
        this.likedUsernames = likedUsernames;
    }

    public String getPostId() {
        return postId;
    }

    public void setPostId(String postId) {
        this.postId = postId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
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

    public int getLikeCount() {
        return likeCount;
    }

    public void setLikeCount(int likeCount) {
        this.likeCount = likeCount;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }

    public List<Comment> getComments() {
        return comments;
    }

    public void setComments(List<Comment> comments) {
        this.comments = comments;
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

    public String getTimeAgo() {
        if (date == null || date.isEmpty()) return "Không xác định";

        // Chuyển từ String sang LocalDateTime
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        LocalDateTime postTime = LocalDateTime.parse(date, formatter);
        LocalDateTime now = LocalDateTime.now();

        // Tính khoảng cách giữa hai thời điểm
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
}
