package com.example.exambuddy.service;

import com.example.exambuddy.config.FirebaseConfig;
import com.example.exambuddy.model.Chapter;
import com.example.exambuddy.model.Lesson;
import com.example.exambuddy.model.Subject;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.example.exambuddy.model.Class;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class TheoryService {


    @Autowired
    private FirebaseConfig firebaseConfig;

    // Lấy danh sách lớp
    public List<Class> getClasses() throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        QuerySnapshot querySnapshot = firestore.collection("classes").get().get();
        return querySnapshot.getDocuments().stream()
                .map(document -> document.toObject(Class.class))
                .collect(Collectors.toList());
    }

    // Thêm lớp
    public void addClass(Class cls) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        firestore.collection("classes")
                .document(cls.getId())
                .set(cls)
                .get();
    }

    // Xóa lớp
    public void deleteClass(String classId) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        firestore.collection("classes")
                .document(classId)
                .delete()
                .get();
    }

    // Lấy danh sách môn học
    public List<Subject> getSubjects(String classId) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        QuerySnapshot querySnapshot = firestore.collection("classes")
                .document(classId)
                .collection("subjects")
                .get()
                .get();
        return querySnapshot.getDocuments().stream()
                .map(document -> document.toObject(Subject.class))
                .collect(Collectors.toList());
    }

    // Thêm môn học
    public void addSubject(String classId, Subject subject) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        firestore.collection("classes")
                .document(classId)
                .collection("subjects")
                .document(subject.getId())
                .set(subject)
                .get();
    }

    // Xóa môn học
    public void deleteSubject(String classId, String subjectId) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        firestore.collection("classes")
                .document(classId)
                .collection("subjects")
                .document(subjectId)
                .delete()
                .get();
    }

    // Lấy danh sách chương
    public List<Chapter> getChapters(String subjectId) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        QuerySnapshot querySnapshot = firestore.collection("subjects")
                .document(subjectId)
                .collection("chapters")
                .get()
                .get();
        return querySnapshot.getDocuments().stream()
                .map(document -> document.toObject(Chapter.class))
                .collect(Collectors.toList());
    }

    // Thêm chương
    public void addChapter(String subjectId, Chapter chapter) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        firestore.collection("subjects")
                .document(subjectId)
                .collection("chapters")
                .document(chapter.getId())
                .set(chapter)
                .get();
    }

    // Xóa chương
    public void deleteChapter(String subjectId, String chapterId) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        firestore.collection("subjects")
                .document(subjectId)
                .collection("chapters")
                .document(chapterId)
                .delete()
                .get();
    }

    // Lấy danh sách bài học
    public List<Lesson> getLessons(String chapterId) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        QuerySnapshot querySnapshot = firestore.collection("chapters")
                .document(chapterId)
                .collection("lessons")
                .get()
                .get();
        return querySnapshot.getDocuments().stream()
                .map(document -> document.toObject(Lesson.class))
                .collect(Collectors.toList());
    }

    // Thêm bài học
    public void addLesson(String chapterId, Lesson lesson) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        firestore.collection("chapters")
                .document(chapterId)
                .collection("lessons")
                .document(lesson.getId())
                .set(lesson)
                .get();
    }

    // Xóa bài học
    public void deleteLesson(String chapterId, String lessonId) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        firestore.collection("chapters")
                .document(chapterId)
                .collection("lessons")
                .document(lessonId)
                .delete()
                .get();
    }

    // Cập nhật nội dung bài học
    public void updateLessonContent(String lessonId, String content) throws ExecutionException, InterruptedException {
        Firestore firestore = firebaseConfig.getFirestore();
        firestore.collection("lessons")
                .document(lessonId)
                .update("content", content)
                .get();
    }
}
