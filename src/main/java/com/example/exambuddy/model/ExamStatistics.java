package com.example.exambuddy.model;

import java.util.Map;

public class ExamStatistics {
    private int participantCount;       // Số người tham gia
    private double averageScore;        // Điểm trung bình
    private double minScore;            // Điểm thấp nhất
    private double maxScore;            // Điểm cao nhất
    private Map<String, Integer> scoreDistribution; // Phân phối điểm


    public ExamStatistics(int participantCount, double averageScore, double minScore, double maxScore, Map<String, Integer> scoreDistribution) {
        this.participantCount = participantCount;
        this.averageScore = averageScore;
        this.minScore = minScore;
        this.maxScore = maxScore;
        this.scoreDistribution = scoreDistribution;
    }

    public int getParticipantCount() {
        return participantCount;
    }

    public void setParticipantCount(int participantCount) {
        this.participantCount = participantCount;
    }

    public Map<String, Integer> getScoreDistribution() {
        return scoreDistribution;
    }

    public void setScoreDistribution(Map<String, Integer> scoreDistribution) {
        this.scoreDistribution = scoreDistribution;
    }

    public double getMinScore() {
        return minScore;
    }

    public void setMinScore(double minScore) {
        this.minScore = minScore;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public double getAverageScore() {
        return averageScore;
    }

    public void setAverageScore(double averageScore) {
        this.averageScore = averageScore;
    }
}
