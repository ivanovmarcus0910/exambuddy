package com.example.exambuddy.service;

import com.example.exambuddy.config.FirebaseConfig;
import com.example.exambuddy.model.Chapter;
import com.example.exambuddy.model.Lesson;
import com.example.exambuddy.model.Subject;
import com.google.cloud.firestore.*;

import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Service;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.springframework.beans.factory.annotation.Autowired;


import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class TheoryService {
    @Autowired
    private FirebaseConfig firebaseConfig;

    @Autowired
    private CloudinaryService cloudinary;



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
    public String extractContentFromFile(File file) throws IOException {
        String fileName = file.getName().toLowerCase();
        StringBuilder content = new StringBuilder();

        if (fileName.endsWith(".docx")) {
            content.append(extractFromWord(file));
        } else if (fileName.endsWith(".pdf")) {
            content.append(extractFromPDF(file));
        } else if (fileName.endsWith(".doc")) {
            throw new IllegalArgumentException("File .doc chưa được hỗ trợ, vui lòng dùng .docx.");
        } else {
            throw new IllegalArgumentException("Định dạng file không hỗ trợ. Chỉ hỗ trợ .docx và .pdf.");
        }

        return content.toString().trim();
    }

    /**
     * Trích xuất văn bản và ảnh từ file Word, giữ nguyên định dạng và vị trí
     */
    private String extractFromWord(File file) throws IOException {
        StringBuilder content = new StringBuilder();

        try (FileInputStream fis = new FileInputStream(file);
             XWPFDocument document = new XWPFDocument(fis)) {

            // Duyệt qua từng đoạn văn (paragraph)
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                StringBuilder paragraphContent = new StringBuilder();
                boolean hasImage = false;

                // Duyệt qua từng run trong đoạn văn
                for (XWPFRun run : paragraph.getRuns()) {
                    // Lấy văn bản từ run, giữ nguyên khoảng trắng, tab, và xuống dòng
                    String text = run.getText(0);
                    if (text != null && !text.isEmpty()) {
                        // Giữ nguyên các ký tự xuống dòng, tab, và khoảng trắng
                        text = text.replaceAll("\r\n|\r", "<br>")
                                .replaceAll("\t", "&nbsp;&nbsp;&nbsp;") // Giữ tab bằng cách sử dụng khoảng trắng không ngắt
                                .replaceAll("  ", "&nbsp;&nbsp;"); // Giữ khoảng trắng
                        paragraphContent.append(text);
                    }

                    // Kiểm tra và xử lý ảnh trong run, giữ vị trí ảnh
                    if (!run.getEmbeddedPictures().isEmpty()) {
                        hasImage = true;
                        for (var picture : run.getEmbeddedPictures()) {
                            byte[] pictureData = picture.getPictureData().getData();
                            String imageUrl = uploadImageToCloudinary(pictureData, "theory_images");
                            if (imageUrl != null) {
                                paragraphContent.append("\n<img src=\"").append(imageUrl)
                                        .append("\" alt=\"Image from Word\" style=\"display: block; margin: 10px 0; max-width: 100%;\">\n");
                            }
                        }
                    }
                }

                // Thêm đoạn văn vào nội dung, giữ định dạng
                if (paragraphContent.length() > 0) {
                    content.append("<p>").append(paragraphContent.toString()).append("</p>");
                    if (hasImage) {
                        content.append("\n"); // Thêm khoảng cách sau ảnh
                    }
                }
            }

            // Xử lý bảng, giữ cấu trúc và định dạng
            for (XWPFTable table : document.getTables()) {
                content.append("<table border=\"1\" style=\"border-collapse: collapse; margin: 10px 0; width: 100%;\">");
                for (XWPFTableRow row : table.getRows()) {
                    content.append("<tr>");
                    for (XWPFTableCell cell : row.getTableCells()) {
                        content.append("<td style=\"padding: 5px; border: 1px solid #000;\">");
                        String cellText = cell.getText().replaceAll("\r\n|\r", "<br>")
                                .replaceAll("\t", "&nbsp;&nbsp;&nbsp;")
                                .replaceAll("  ", "&nbsp;&nbsp;");
                        content.append(cellText);
                        content.append("</td>");
                    }
                    content.append("</tr>");
                }
                content.append("</table>");
            }
        }
        return content.toString().trim();
    }

    /**
     * Trích xuất văn bản và ảnh từ file PDF, giữ layout tốt hơn
     */
    private String extractFromPDF(File file) throws IOException {
        StringBuilder content = new StringBuilder();

        try (PDDocument document = PDDocument.load(file)) {
            PDFRenderer pdfRenderer = new PDFRenderer(document);

            for (int page = 0; page < document.getNumberOfPages(); ++page) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300); // 300 DPI
                String imageUrl = uploadImageToCloudinary(bufferedImageToByteArray(image), "theory_images");
                if (imageUrl != null) {
                    content.append("\n<img src=\"").append(imageUrl)
                            .append("\" alt=\"Image from PDF page ").append(page + 1)
                            .append("\" style=\"display: block; margin: 2px 0; max-width: 100%;\">\n");
                }
            }
        }
        return content.toString().trim();
    }

    /**
     * Upload ảnh lên Cloudinary và trả về URL
     */
    private String uploadImageToCloudinary(byte[] imageData, String folderName) {
        try {
            String url = cloudinary.uploadImgFromBytes(imageData, folderName);
            return url;
        } catch (Exception e) {
            System.out.println("Image upload fail: " + e.getMessage());
            return null;
        }
    }

    /**
     * Chuyển BufferedImage thành byte array
     */
    private byte[] bufferedImageToByteArray(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", baos);
        return baos.toByteArray();
    }




    public void updateSubject(String classId, String subjectId, Subject updatedSubject) throws ExecutionException, InterruptedException {
        System.out.println("Updating subjectId: " + subjectId + " with name: " + updatedSubject.getName());
        DocumentReference subjectRef = getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId);

        DocumentSnapshot subjectSnapshot = subjectRef.get().get();
        if (!subjectSnapshot.exists()) {
            throw new RuntimeException("Môn học không tồn tại!");
        }

        subjectRef.update("name", updatedSubject.getName()).get();
    }

    public void updateChapter(String classId, String subjectId, String chapterId, Chapter updatedChapter) throws ExecutionException, InterruptedException {
        System.out.println("Updating chapterId: " + chapterId + " with name: " + updatedChapter.getName());
        DocumentReference chapterRef = getFirestore().collection("classes").document(classId)
                .collection("subjects").document(subjectId)
                .collection("chapters").document(chapterId);

        DocumentSnapshot chapterSnapshot = chapterRef.get().get();
        if (!chapterSnapshot.exists()) {
            throw new RuntimeException("Chương không tồn tại!");
        }

        chapterRef.update("name", updatedChapter.getName()).get();
    }
}


