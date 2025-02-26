package com.example.exambuddy.model;

public class Payment {
    long paymentCode;
    int amount;
    String url;
    Long timestamp;
    String status;
    String username;

    public Payment(long paymentCode, int amount, String url, String status, String username, Long timestamp) {
        this.paymentCode = paymentCode;
        this.amount = amount;
        this.url = url;
        this.status = status;
        this.username = username;
        this.timestamp = timestamp;
    }

    public long getPaymentCode() {
        return paymentCode;
    }

    public void setPaymentCode(long paymentCode) {
        this.paymentCode = paymentCode;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
