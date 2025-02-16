    package com.example.exambuddy.model;

    import org.springframework.context.annotation.Bean;

    import java.util.List;
    import java.util.Map;

    public class ExamResult {
        private String examID;
        private double score;
        private Map<String, Object> userAnswers;
        private long submittedAt;
        private List<String> correctAnswers;
        public ExamResult() {
        }

        public ExamResult(String examID, double score, Map<String, Object> userAnswers, long submittedAt, List<String> correctAnswers) {
            this.examID = examID;
            this.score = score;
            this.userAnswers = userAnswers;
            this.submittedAt = submittedAt;
            this.correctAnswers = correctAnswers;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public Map<String, Object> getUserAnswers() {
            return userAnswers;
        }

        public void setUserAnswers(Map<String, Object> userAnswers) {
            this.userAnswers = userAnswers;
        }

        public long getSubmittedAt() {
            return submittedAt;
        }

        public void setSubmittedAt(long submittedAt) {
            this.submittedAt = submittedAt;
        }

        public String getExamID() {
            return examID;
        }

        public void setExamID(String examID) {
            this.examID = examID;
        }

        public List<String> getCorrectAnswers() {
            return correctAnswers;
        }

        public void setCorrectAnswers(List<String> correctAnswers) {
            this.correctAnswers = correctAnswers;
        }
    }
