package com.example.exambuddy.model;


import java.util.Date;

public class User {
    private String id;
    private String email;
    private String phone;
    private String username;
    private String password;
    private boolean verified;
    private String avatarUrl;
    private String role;
    private int coin;
    private long joinDate;


    // Thuộc tính mới: trạng thái hoạt động
    private boolean active = true;

    private String firstName;
    private String lastName;
    private String birthDate;
    private String address;
    private String grade;
    private String studentId;
    private String description;
    private long timeExpriredPremium;
    // Các trường dành cho giáo viên
    private String teacherCode;
    private String school;
    private String speciality;
    private Integer experience; // Số năm kinh nghiệm
    private String degreeUrl;   // Link đến bằng cấp/chứng chỉ đã tải lên
    public enum Role {
        ADMIN, STUDENT, TEACHER, UPGRADED_STUDENT, PENDING_TEACHER
    }
    private long joinTime ;
    public User() {

        this.role = Role.STUDENT.name();
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
        this.joinTime = System.currentTimeMillis();
        this.active = true; // Người dùng mới mặc định hoạt động
        this.timeExpriredPremium = 0;
    }

    public long getJoinDate() { return joinDate;}

    public void setJoinDate(long joinDate) { this.joinDate = joinDate;}

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
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


    public int getCoin() {
        return coin;
    }

    public void setCoin(int coin) {
        this.coin = coin;
    }

    // Getters và setters cho các trường mới
    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(String birthDate) {
        this.birthDate = birthDate;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public String getGrade() {
        return grade;
    }

    public void setGrade(String grade) {
        this.grade = grade;
    }

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    // Getters và Setters cho các trường dành cho giáo viên
    public String getTeacherCode() {
        return teacherCode;
    }

    public void setTeacherCode(String teacherCode) {
        this.teacherCode = teacherCode;
    }

    public String getSchool() {
        return school;
    }

    public void setSchool(String school) {
        this.school = school;
    }

    public String getSpeciality() {
        return speciality;
    }

    public void setSpeciality(String speciality) {
        this.speciality = speciality;
    }

    public Integer getExperience() {
        return experience;
    }

    public void setExperience(Integer experience) {
        this.experience = experience;
    }

    public String getDegreeUrl() {
        return degreeUrl;
    }

    public void setDegreeUrl(String degreeUrl) {
        this.degreeUrl = degreeUrl;
    }

    public long getJoinTime() {
        return joinTime;
    }

    public void setJoinTime(long joinTime) {
        this.joinTime = joinTime;
    }

    public long getTimeExpriredPremium() {
        return timeExpriredPremium;
    }

    public void setTimeExpriredPremium(long timeExpriredPremium) {
        this.timeExpriredPremium = timeExpriredPremium;
    }
}
