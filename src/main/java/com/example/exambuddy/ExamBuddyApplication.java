package com.example.exambuddy;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;


@SpringBootApplication(scanBasePackages = "com.example.exambuddy")
@EnableAsync
public class ExamBuddyApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExamBuddyApplication.class, args);
    }

}
