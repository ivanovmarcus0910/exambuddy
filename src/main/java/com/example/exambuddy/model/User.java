package com.example.exambuddy.model;

public class User {
    private String id;
    private String email;
    private String phone;
    private String username;
    private String password;
    private boolean verified;
    private String verificationToken;

    public User() {}

    public User(String id, String email, String phone, String username, String password, String verificationToken) {
        this.id = id;
        this.email = email;
        this.phone = phone;
        this.username = username;
        this.password = password;
        this.verificationToken = verificationToken;
        this.verified = false; // Mặc định chưa xác thực
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
    }

    public String getVerificationToken() {
        return verificationToken;
    }

    public void setVerificationToken(String verificationToken) {
        this.verificationToken = verificationToken;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
