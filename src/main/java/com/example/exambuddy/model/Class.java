package com.example.exambuddy.model;

import java.util.List;

public class Class {
    private String id;
    private String name;
    private List<Subject> subjects;

    public Class() {
    }

    public Class(String id, List<Subject> subjects, String name) {
        this.id = id;
        this.subjects = subjects;
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

    public List<Subject> getSubjects() {
        return subjects;
    }

    public void setSubjects(List<Subject> subjects) {
        this.subjects = subjects;
    }
}