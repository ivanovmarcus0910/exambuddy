package com.example.exambuddy.controller;

import com.example.exambuddy.model.Chapter;
import com.example.exambuddy.model.Lesson;
import com.example.exambuddy.model.Subject;
import com.example.exambuddy.model.Class;
import com.example.exambuddy.service.TheoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/theory")
public class TheoryController {
    @Autowired
    private TheoryService theoryService;

    // Lấy danh sách lớp
    @GetMapping("/classes")
    public List<Class> getClasses() throws ExecutionException, InterruptedException {
        return theoryService.getClasses();
    }

    // Thêm lớp
    @PostMapping("/classes")
    public void addClass(@RequestBody Class cls) throws ExecutionException, InterruptedException {
        theoryService.addClass(cls);
    }

    // Xóa lớp
    @DeleteMapping("/classes/{classId}")
    public void deleteClass(@PathVariable String classId) throws ExecutionException, InterruptedException {
        theoryService.deleteClass(classId);
    }

    // Lấy danh sách môn học
    @GetMapping("/subjects/{classId}")
    public List<Subject> getSubjects(@PathVariable String classId) throws ExecutionException, InterruptedException {
        return theoryService.getSubjects(classId);
    }

    // Thêm môn học
    @PostMapping("/subjects/{classId}")
    public void addSubject(@PathVariable String classId, @RequestBody Subject subject) throws ExecutionException, InterruptedException {
        theoryService.addSubject(classId, subject);
    }

    // Xóa môn học
    @DeleteMapping("/subjects/{classId}/{subjectId}")
    public void deleteSubject(@PathVariable String classId, @PathVariable String subjectId) throws ExecutionException, InterruptedException {
        theoryService.deleteSubject(classId, subjectId);
    }

    // Lấy danh sách chương
    @GetMapping("/chapters/{classId}/{subjectId}")
    public List<Chapter> getChapters(@PathVariable String classId, @PathVariable String subjectId) throws ExecutionException, InterruptedException {
        return theoryService.getChapters(classId, subjectId);
    }

    // Thêm chương
    @PostMapping("/chapters/{classId}/{subjectId}")
    public void addChapter(@PathVariable String classId, @PathVariable String subjectId, @RequestBody Chapter chapter) throws ExecutionException, InterruptedException {
        theoryService.addChapter(classId, subjectId, chapter);
    }

    // Xóa chương
    @DeleteMapping("/chapters/{classId}/{subjectId}/{chapterId}")
    public void deleteChapter(@PathVariable String classId, @PathVariable String subjectId, @PathVariable String chapterId) throws ExecutionException, InterruptedException {
        theoryService.deleteChapter(classId, subjectId, chapterId);
    }

    // Lấy danh sách bài học
    @GetMapping("/lessons/{classId}/{subjectId}/{chapterId}")
    public List<Lesson> getLessons(@PathVariable String classId, @PathVariable String subjectId, @PathVariable String chapterId) throws ExecutionException, InterruptedException {
        return theoryService.getLessons(classId, subjectId, chapterId);
    }

    // Thêm bài học
    @PostMapping("/lessons/{classId}/{subjectId}/{chapterId}")
    public void addLesson(@PathVariable String classId, @PathVariable String subjectId, @PathVariable String chapterId, @RequestBody Lesson lesson) throws ExecutionException, InterruptedException {
        theoryService.addLesson(classId, subjectId, chapterId, lesson);
    }

    // Xóa bài học
    @DeleteMapping("/lessons/{classId}/{subjectId}/{chapterId}/{lessonId}")
    public void deleteLesson(@PathVariable String classId, @PathVariable String subjectId, @PathVariable String chapterId, @PathVariable String lessonId) throws ExecutionException, InterruptedException {
        theoryService.deleteLesson(classId, subjectId, chapterId, lessonId);
    }

    // Cập nhật nội dung bài học
    @PutMapping("/lessons/{classId}/{subjectId}/{chapterId}/{lessonId}")
    public void updateLessonContent(@PathVariable String classId, @PathVariable String subjectId, @PathVariable String chapterId, @PathVariable String lessonId, @RequestBody String content) throws ExecutionException, InterruptedException {
        theoryService.updateLessonContent(classId, subjectId, chapterId, lessonId, content);
    }

    // Lấy bài học theo ID
    @GetMapping("/lesson/{classId}/{subjectId}/{chapterId}/{lessonId}")
    public Lesson getLessonById(@PathVariable String classId, @PathVariable String subjectId, @PathVariable String chapterId, @PathVariable String lessonId) throws ExecutionException, InterruptedException {
        return theoryService.getLessonById(classId, subjectId, chapterId, lessonId);
    }
}
