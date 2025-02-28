package com.example.exambuddy.model;

import java.util.List;

public class Class {
    private String id;
    private String name;
    private String bookType;
    private List<Subject> subjects;

    public Class() {
    }

    public Class(String id, String bookType, List<Subject> subjects, String name) {
        this.id = id;
        this.subjects = subjects;
        this.bookType = bookType;
        this.name = name;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBookType() {
        return bookType;
    }

    public void setBookType(String bookType) {
        this.bookType = bookType;
    }

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }
}