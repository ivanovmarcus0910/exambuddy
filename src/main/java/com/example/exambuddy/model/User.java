package com.example.exambuddy.model;

public class User {
    private String id;
    private String email;
    private String phone;
    private String username;
    private String password;
    private boolean verified;
    private String avatarUrl;
    private String role;
    private String fullName;
    private int coin;

    public enum Role {
        ADMIN, USER, TEACHER, UPGRADED_USER
    }

    public User() {
        this.role = Role.USER.name();
    }

    public User(String id, String email, String username, String password, boolean verified, Role role) {
        this.id = id;
        this.email = email;
        this.phone = "";
        this.username = username;
        this.password = password;
        this.verified = false; // Mặc định chưa xác thực
        this.avatarUrl = "https://res.cloudinary.com/dsuav027e/image/upload/v1739367972/halnqohla5mqr3seve3d.png";
        this.role = role.name();
        this.coin = 0;
    }

    public boolean isVerified() {
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
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

    public String getAvatarUrl() {
        return avatarUrl;
    }

    public void setAvatarUrl(String avatarUrl) {
        this.avatarUrl = avatarUrl;
    }

    public Role getRole() {
        return Role.valueOf(this.role);
    }

    public void setRole(Role role) {
        this.role = role.name();
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public int getCoin() {
        return coin;
    }

    public void setCoin(int coin) {
        this.coin = coin;
    }
}
