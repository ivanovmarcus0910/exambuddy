package com.example.exambuddy.model;


import java.util.List;

public class Chapter {
    private String id;
    private String name;
    private List<Lesson> lessons;


    public Chapter() {
    }

    public Chapter(String id, String name, List<Lesson> lessons) {
        this.id = id;
        this.name = name;
        this.lessons = lessons;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<Lesson> getLessons() {
        return lessons;
    }

    public void setLessons(List<Lesson> lessons) {
        this.lessons = lessons;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
