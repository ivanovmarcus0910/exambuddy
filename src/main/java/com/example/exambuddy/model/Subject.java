package com.example.exambuddy.model;


import java.util.List;

public class Subject {
    private String id;
    private String name;
    private List<Chapter> chapters;

    public Subject() {
    }

    public Subject(String id, String name, List<Chapter> chapters) {
        this.id = id;
        this.name = name;
        this.chapters = chapters;
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

    public List<Chapter> getChapters() {
        return chapters;
    }

    public void setChapters(List<Chapter> chapters) {
        this.chapters = chapters;
    }
}
