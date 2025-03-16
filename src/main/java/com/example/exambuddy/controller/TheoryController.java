package com.example.exambuddy.controller;

import com.example.exambuddy.model.Chapter;
import com.example.exambuddy.model.Lesson;
import com.example.exambuddy.model.Subject;
import com.example.exambuddy.service.TheoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/api/theory")
public class TheoryController {
    @Autowired
    private TheoryService theoryService;

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
    public ResponseEntity<String> updateLessonContent(@PathVariable String classId, @PathVariable String subjectId, @PathVariable String chapterId, @PathVariable String lessonId, @RequestBody Lesson lesson) {
        System.out.println("Received update request for lessonId: " + lessonId + " with content: " + lesson.getContent());
        try {
            theoryService.updateLessonContent(classId, subjectId, chapterId, lessonId, lesson.getContent());
            return ResponseEntity.ok("Nội dung bài học đã được cập nhật thành công!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Lỗi khi cập nhật nội dung bài học: " + e.getMessage());
        }
    }

    // Lấy bài học theo ID
    @GetMapping("/lesson/{classId}/{subjectId}/{chapterId}/{lessonId}")
    public Lesson getLessonById(@PathVariable String classId, @PathVariable String subjectId, @PathVariable String chapterId, @PathVariable String lessonId) throws ExecutionException, InterruptedException {
        return theoryService.getLessonById(classId, subjectId, chapterId, lessonId);
    }

    @PostMapping("/import")
    public String importFile(@RequestParam("file") MultipartFile multipartFile) throws IOException {
        File file = File.createTempFile("temp", multipartFile.getOriginalFilename());
        multipartFile.transferTo(file);
        return theoryService.extractContentFromFile(file);
    }

    @PutMapping("/subjects/{classId}/{subjectId}")
    public ResponseEntity<String> updateSubject(
            @PathVariable String classId,
            @PathVariable String subjectId,
            @RequestBody Subject updatedSubject) {
        try {
            theoryService.updateSubject(classId, subjectId, updatedSubject);
            return ResponseEntity.ok("Môn học đã được cập nhật thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật môn học: " + e.getMessage());
        }
    }

    @PutMapping("/chapters/{classId}/{subjectId}/{chapterId}")
    public ResponseEntity<String> updateChapter(
            @PathVariable String classId,
            @PathVariable String subjectId,
            @PathVariable String chapterId,
            @RequestBody Chapter updatedChapter) {
        try {
            theoryService.updateChapter(classId, subjectId, chapterId, updatedChapter);
            return ResponseEntity.ok("Chương đã được cập nhật thành công!");
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Lỗi khi cập nhật chương: " + e.getMessage());
        }
    }

}