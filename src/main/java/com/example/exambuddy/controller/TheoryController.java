package com.example.exambuddy.controller;
import com.example.exambuddy.model.Class;
import com.example.exambuddy.model.Chapter;
import com.example.exambuddy.model.Lesson;
import com.example.exambuddy.model.Subject;
import com.example.exambuddy.service.TheoryService;
import com.google.firebase.internal.FirebaseService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api")
public class TheoryController {


    private final TheoryService firebaseService;

    public TheoryController(TheoryService firebaseService) {
        this.firebaseService = firebaseService;
    }

    // Lấy danh sách lớp
    @GetMapping("/classes")
    public List<Class> getClasses() throws ExecutionException, InterruptedException {
        return firebaseService.getClasses();
    }

    // Thêm lớp
    @PostMapping("/classes")
    public void addClass(@RequestBody Class cls) throws ExecutionException, InterruptedException {
        firebaseService.addClass(cls);
    }

    // Xóa lớp
    @DeleteMapping("/classes/{classId}")
    public void deleteClass(@PathVariable String classId) throws ExecutionException, InterruptedException {
        firebaseService.deleteClass(classId);
    }

    // Lấy danh sách môn học
    @GetMapping("/classes/{classId}/subjects")
    public List<Subject> getSubjects(@PathVariable String classId) throws ExecutionException, InterruptedException {
        return firebaseService.getSubjects(classId);
    }

    // Thêm môn học
    @PostMapping("/classes/{classId}/subjects")
    public void addSubject(@PathVariable String classId, @RequestBody Subject subject) throws ExecutionException, InterruptedException {
        firebaseService.addSubject(classId, subject);
    }

    // Xóa môn học
    @DeleteMapping("/classes/{classId}/subjects/{subjectId}")
    public void deleteSubject(@PathVariable String classId, @PathVariable String subjectId) throws ExecutionException, InterruptedException {
        firebaseService.deleteSubject(classId, subjectId);
    }

    // Lấy danh sách chương
    @GetMapping("/subjects/{subjectId}/chapters")
    public List<Chapter> getChapters(@PathVariable String subjectId) throws ExecutionException, InterruptedException {
        return firebaseService.getChapters(subjectId);
    }

    // Thêm chương
    @PostMapping("/subjects/{subjectId}/chapters")
    public void addChapter(@PathVariable String subjectId, @RequestBody Chapter chapter) throws ExecutionException, InterruptedException {
        firebaseService.addChapter(subjectId, chapter);
    }

    // Xóa chương
    @DeleteMapping("/subjects/{subjectId}/chapters/{chapterId}")
    public void deleteChapter(@PathVariable String subjectId, @PathVariable String chapterId) throws ExecutionException, InterruptedException {
        firebaseService.deleteChapter(subjectId, chapterId);
    }

    // Lấy danh sách bài học
    @GetMapping("/chapters/{chapterId}/lessons")
    public List<Lesson> getLessons(@PathVariable String chapterId) throws ExecutionException, InterruptedException {
        return firebaseService.getLessons(chapterId);
    }

    // Thêm bài học
    @PostMapping("/chapters/{chapterId}/lessons")
    public void addLesson(@PathVariable String chapterId, @RequestBody Lesson lesson) throws ExecutionException, InterruptedException {
        firebaseService.addLesson(chapterId, lesson);
    }

    // Xóa bài học
    @DeleteMapping("/chapters/{chapterId}/lessons/{lessonId}")
    public void deleteLesson(@PathVariable String chapterId, @PathVariable String lessonId) throws ExecutionException, InterruptedException {
        firebaseService.deleteLesson(chapterId, lessonId);
    }

    // Cập nhật nội dung bài học
    @PostMapping("/lessons/{lessonId}/content")
    public void updateLessonContent(@PathVariable String lessonId, @RequestBody String content) throws ExecutionException, InterruptedException {
        firebaseService.updateLessonContent(lessonId, content);
    }
}