package com.example.exambuddy.model;

public class RecordTopUser {
    private String username;
    private double totalScore;

    public RecordTopUser() {
    }
    public RecordTopUser(String username, double totalScore) {
        this.username = username;
        this.totalScore = totalScore;
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

}
