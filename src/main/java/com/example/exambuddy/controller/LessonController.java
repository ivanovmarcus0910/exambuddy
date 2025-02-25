package com.example.exambuddy.controller;

import com.example.exambuddy.model.Lesson;
import com.example.exambuddy.service.TheoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.concurrent.ExecutionException;

@Controller
public class LessonController {

    private final TheoryService theoryService;

    public LessonController(TheoryService firebaseService) {
        this.theoryService = firebaseService;
    }

    @GetMapping("/lesson")
    public String getLessonPage(
            @RequestParam String classId,
            @RequestParam String subjectId,
            @RequestParam String chapterId,
            @RequestParam String lessonId,
            Model model) throws ExecutionException, InterruptedException {

        System.out.println("Lấy bài học với Class ID: " + classId +
                ", Subject ID: " + subjectId +
                ", Chapter ID: " + chapterId +
                " và Lesson ID: " + lessonId);

        Lesson lesson = theoryService.getLessonById(classId, subjectId, chapterId, lessonId);

        if (lesson != null) {
            model.addAttribute("lesson", lesson);
            System.out.println("Nội dung bài học: " + lesson.getContent());
        } else {
            model.addAttribute("error", "Bài học không tồn tại!");
            System.out.println("Không tìm thấy bài học!");
        }

        return "lesson";  // Trả về lesson.html
    }
}
