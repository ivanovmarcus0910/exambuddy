package com.example.exambuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.example.exambuddy")
public class ExamBuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamBuddyApplication.class, args);
    }

}
