package com.example.exambuddy.model;

public class RecordTopUser {
    private String username;
    private double totalScore;
    private String avatarUrl;

    public RecordTopUser() {
    }
    public RecordTopUser(String username, double totalScore , String avatarUrl) {
        this.username = username;
        this.totalScore = totalScore;
        this.avatarUrl = avatarUrl;
    }
    public String getUsername() {
        return username;
    }
    public void setUsername(String username) {
        this.username = username;
    }
    public double getTotalScore() {
        return totalScore;
    }
    public void setTotalScore(double totalScore) {
        this.totalScore = totalScore;
    }

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }
}
