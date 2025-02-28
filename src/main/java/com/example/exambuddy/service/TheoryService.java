package com.example.exambuddy.service;

import com.example.exambuddy.config.FirebaseConfig;
import com.example.exambuddy.model.Chapter;
import com.example.exambuddy.model.Lesson;
import com.example.exambuddy.model.Subject;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class TheoryService {
    @Autowired
    private FirebaseConfig firebaseConfig;

    private Firestore getFirestore() {
        return firebaseConfig.getFirestore();
    }

    // Lấy danh sách môn học
    public List<Subject> getSubjects(String classId) throws ExecutionException, InterruptedException {
        System.out.println("Fetching subjects for classId: " + classId);
        return getFirestore().collection("classes").document(classId)
                .collection("subjects").get().get().getDocuments()
                .stream().map(doc -> doc.toObject(Subject.class)).collect(Collectors.toList());
    }

    // Thêm môn học
    public void addSubject(String classId, Subject subject) throws ExecutionException, InterruptedException {
        System.out.println("Adding subject: " + subject.getName() + " to classId: " + classId);
        getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subject.getId()).set(subject).get();
    }

    // Xóa môn học
    public void deleteSubject(String classId, String subjectId) throws ExecutionException, InterruptedException {
        System.out.println("Deleting subjectId: " + subjectId + " from classId: " + classId);
        getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId).delete().get();
    }

    // Lấy danh sách chương
    public List<Chapter> getChapters(String classId, String subjectId) throws ExecutionException, InterruptedException {
        System.out.println("Fetching chapters for classId: " + classId + ", subjectId: " + subjectId);
        return getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId)
                .collection("chapters").get().get().getDocuments()
                .stream().map(doc -> doc.toObject(Chapter.class)).collect(Collectors.toList());
    }

    // Thêm chương
    public void addChapter(String classId, String subjectId, Chapter chapter) throws ExecutionException, InterruptedException {
        System.out.println("Adding chapter: " + chapter.getName() + " to classId: " + classId + ", subjectId: " + subjectId);
        getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId)
                .collection("chapters").document(chapter.getId()).set(chapter).get();
    }

    // Xóa chương
    public void deleteChapter(String classId, String subjectId, String chapterId) throws ExecutionException, InterruptedException {
        System.out.println("Deleting chapterId: " + chapterId + " from classId: " + classId + ", subjectId: " + subjectId);
        getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId)
                .collection("chapters").document(chapterId).delete().get();
    }

    // Lấy danh sách bài học
    public List<Lesson> getLessons(String classId, String subjectId, String chapterId) throws ExecutionException, InterruptedException {
        System.out.println("Fetching lessons for classId: " + classId + ", subjectId: " + subjectId + ", chapterId: " + chapterId);
        return getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId)
                .collection("chapters").document(chapterId)
                .collection("lessons").get().get().getDocuments()
                .stream().map(doc -> doc.toObject(Lesson.class)).collect(Collectors.toList());
    }

    // Thêm bài học
    public void addLesson(String classId, String subjectId, String chapterId, Lesson lesson) throws ExecutionException, InterruptedException {
        System.out.println("Adding lesson: " + lesson.getName() + " to classId: " + classId + ", subjectId: " + subjectId + ", chapterId: " + chapterId);
        getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId)
                .collection("chapters").document(chapterId)
                .collection("lessons").document(lesson.getId()).set(lesson).get();
    }

    // Xóa bài học
    public void deleteLesson(String classId, String subjectId, String chapterId, String lessonId) throws ExecutionException, InterruptedException {
        System.out.println("Deleting lessonId: " + lessonId + " from classId: " + classId + ", subjectId: " + subjectId + ", chapterId: " + chapterId);
        getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId)
                .collection("chapters").document(chapterId)
                .collection("lessons").document(lessonId).delete().get();
    }

    // Cập nhật nội dung bài học
    public void updateLessonContent(String classId, String subjectId, String chapterId, String lessonId, String content) throws ExecutionException, InterruptedException {
        System.out.println("Updating lessonId: " + lessonId + " with content: " + content);
        DocumentReference lessonRef = getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId)
                .collection("chapters").document(chapterId)
                .collection("lessons").document(lessonId);

        DocumentSnapshot lessonSnapshot = lessonRef.get().get();

        if (!lessonSnapshot.exists()) {
            throw new RuntimeException("Bài học không tồn tại!");
        }

        lessonRef.update("content", content).get();
    }

    // Lấy bài học theo ID
    public Lesson getLessonById(String classId, String subjectId, String chapterId, String lessonId) throws ExecutionException, InterruptedException {
        System.out.println("Fetching lessonId: " + lessonId + " from classId: " + classId + ", subjectId: " + subjectId + ", chapterId: " + chapterId);
        DocumentSnapshot document = getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId)
                .collection("chapters").document(chapterId)
                .collection("lessons").document(lessonId).get().get();

        return document.exists() ? document.toObject(Lesson.class) : null;
    }
}