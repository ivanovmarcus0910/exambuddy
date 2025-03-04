package com.example.exambuddy.service;

import com.example.exambuddy.model.Post;
import com.example.exambuddy.model.Comment;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
public class PostService {
    private static final Firestore db = FirestoreClient.getFirestore();
    private static final String COLLECTION_NAME = "posts";

    public static Post savePost(String username, String avatarUrl, String content, String subject, String grade, String date, List<String> imageUrls) {
        CollectionReference posts = db.collection(COLLECTION_NAME);

        Post post = new Post();
        post.setUsername(username);
        post.setAvatarUrl(avatarUrl);
        post.setContent(content);
        post.setSubject(subject);
        post.setGrade(grade);
        post.setDate(date);
        post.setImageUrls(imageUrls);
        post.setLikeCount(0);
        post.setLikedUsernames(new ArrayList<>());

        try {
            DocumentReference newPostRef = posts.add(post).get();
            String postId = newPostRef.getId();
            post.setPostId(postId);
            newPostRef.update("postId", postId).get();
            return post;
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("❌ Lỗi khi lưu bài đăng: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static void updateLikeCount(String postId, String username, boolean liked) {
        DocumentReference postRef = db.collection(COLLECTION_NAME).document(postId);
        try {
            DocumentSnapshot document = postRef.get().get();
            if (document.exists()) {
                Post post = document.toObject(Post.class);
                if (post.getLikedUsernames() == null) {
                    post.setLikedUsernames(new ArrayList<>());
                }
                List<String> likedUsernames = post.getLikedUsernames();
                if (liked) {
                    if (!likedUsernames.contains(username)) {
                        post.setLikeCount(post.getLikeCount() + 1);
                        likedUsernames.add(username);
                    }
                } else {
                    if (likedUsernames.contains(username)) {
                        post.setLikeCount(post.getLikeCount() - 1);
                        likedUsernames.remove(username);
                    }
                }
                postRef.update("likeCount", post.getLikeCount(), "likedUsernames", likedUsernames).get();
            } else {
                System.out.println("❌ Không tìm thấy bài đăng với ID: " + postId);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("❌ Lỗi khi cập nhật số lượt thích: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static List<Post> getPostsFromFirestore() {
        List<Post> postList = new ArrayList<>();

        try {
            ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME).get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();
            for (DocumentSnapshot document : documents) {
                Post post = document.toObject(Post.class);
                post.setPostId(document.getId());

                postList.add(post);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return postList;
    }

    public static Comment saveComment(String postId, String username, String avatarUrl, String content, String date, List<String> imageUrls) {

        DocumentReference postRef = db.collection("posts").document(postId);
        CollectionReference commentsRef = postRef.collection("comments");

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUsername(username);
        comment.setAvatarUrl(avatarUrl);
        comment.setContent(content);
        comment.setDate(date);
        comment.setImageUrls(imageUrls);

        try {
            DocumentReference newCommentRef = commentsRef.add(comment).get();
            comment.setCommentId(newCommentRef.getId());
            return comment;
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("❌ Lỗi khi lưu bình luận: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static Post getPostById(String postId) {
        try {
            DocumentReference postRef = db.collection(COLLECTION_NAME).document(postId);
            ApiFuture<DocumentSnapshot> future = postRef.get();
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                return document.toObject(Post.class);
            } else {
                System.out.println("❌ Không tìm thấy bài đăng với ID: " + postId);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("❌ Lỗi khi lấy bài đăng theo ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public static List<Comment> getCommentsByPostId(String postId, String username) {
        List<Comment> comments = new ArrayList<>();
        try {
            long x = System.currentTimeMillis();

            DocumentReference postRef = db.collection("posts").document(postId);
            CollectionReference commentsRef = postRef.collection("comments");
            ApiFuture<QuerySnapshot> future = commentsRef.get();
            System.out.println("In Comment 1: " + (System.currentTimeMillis()-x));

            QuerySnapshot querySnapshot = future.get();
            System.out.println("In Comment 2: " + (System.currentTimeMillis()-x));


            comments = querySnapshot.getDocuments().parallelStream().map(document -> {
                Comment comment = document.toObject(Comment.class);
                comment.setCommentId(document.getId());

                List<String> likedUsers = (List<String>) document.get("likedUsernames");
                comment.setLiked(likedUsers != null && likedUsers.contains(username));

                return comment;
            }).collect(Collectors.toList());
            System.out.println("In Comment 3: " + (System.currentTimeMillis()-x));
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return comments;
    }

    public static Comment getCommentById(String postId, String commentId) {
        try {
            DocumentReference commentRef = db.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document(commentId);
            ApiFuture<DocumentSnapshot> future = commentRef.get();
            DocumentSnapshot document = future.get();
            if (document.exists()) {
                Comment comment = document.toObject(Comment.class);
                comment.setCommentId(document.getId()); // Gán ID cho comment
                return comment;
            } else {
                System.out.println("❌ Không tìm thấy bình luận với ID: " + commentId + " trong bài viết " + postId);
                return null;
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("❌ Lỗi khi lấy bình luận theo ID: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    public void updatePost(String postId, String username, String content, List<String> imageUrls) {
        DocumentReference postRef = db.collection(COLLECTION_NAME).document(postId);

        try {
            DocumentSnapshot document = postRef.get().get();
            if (document.exists()) {
                Post post = document.toObject(Post.class);
                if (post != null && post.getUsername().equals(username)) {
                    postRef.update("content", content, "imageUrls", imageUrls).get();
                    System.out.println("✅ Cập nhật bài viết thành công");
                } else {
                    System.out.println("❌ Không có quyền chỉnh sửa bài viết này");
                }
            } else {
                System.out.println("❌ Không tìm thấy bài viết với ID: " + postId);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Lỗi khi cập nhật bài viết: " + e.getMessage());
        }
    }

    public static boolean deletePost(String postId) {
        Firestore firestore = FirestoreClient.getFirestore();
        try {
            firestore.collection("posts").document(postId).delete();
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    // Phương thức cập nhật trạng thái active của Post (toggle active)
    public static void updatePostStatus(String postId, boolean newStatus) {
        DocumentReference postRef = db.collection(COLLECTION_NAME).document(postId);
        try {
            postRef.update("active", newStatus).get();
            System.out.println("Cập nhật trạng thái active của post " + postId + " thành " + newStatus);
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("Lỗi cập nhật trạng thái của post " + postId);
            e.printStackTrace();
        }
    }

    public static void updateCommentLikeCount(String postId, String commentId, String username, boolean liked) {
        DocumentReference commentRef = db.collection("posts")
                .document(postId)
                .collection("comments")
                .document(commentId);
        try {
            DocumentSnapshot document = commentRef.get().get();
            if (document.exists()) {
                Comment comment = document.toObject(Comment.class);
                if (comment.getLikedUsernames() == null) {
                    comment.setLikedUsernames(new ArrayList<>());
                }
                List<String> likedUsernames = comment.getLikedUsernames();

                if (liked) {
                    if (!likedUsernames.contains(username)) {
                        comment.setLikeCount(comment.getLikeCount() + 1);
                        likedUsernames.add(username);
                    }
                } else {
                    if (likedUsernames.contains(username)) {
                        comment.setLikeCount(comment.getLikeCount() - 1);
                        likedUsernames.remove(username);
                    }
                }
                commentRef.update("likeCount", comment.getLikeCount(), "likedUsernames", likedUsernames).get();
            } else {
                System.out.println("❌ Không tìm thấy bình luận với ID: " + commentId);
            }
        } catch (InterruptedException | ExecutionException e) {
            System.out.println("❌ Lỗi khi cập nhật số lượt thích bình luận: " + e.getMessage());
            e.printStackTrace();
        }
    }

}