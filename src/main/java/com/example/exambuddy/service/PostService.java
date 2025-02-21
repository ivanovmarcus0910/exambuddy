package com.example.exambuddy.service;

import com.example.exambuddy.model.Post;
import com.example.exambuddy.model.Comment;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Service
public class PostService {
    private static final Firestore db = FirestoreClient.getFirestore();
    private static final String COLLECTION_NAME = "posts";

    public static Post savePost(String username, String content, String date, List<String> imageUrls) {
        CollectionReference posts = db.collection(COLLECTION_NAME);

        Post post = new Post();
        post.setUsername(username);
        post.setContent(content);
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

                String avatarUrl = UserService.getAvatarUrlByUsername(post.getUsername());
                post.setAvatarUrl(avatarUrl);

                postList.add(post);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return postList;
    }

    public static Comment saveComment(String postId, String username, String content, String date, List<String> imageUrls) {

        DocumentReference postRef = db.collection("posts").document(postId);
        CollectionReference commentsRef = postRef.collection("comments");

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setUsername(username);
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

        public static List<Comment> getCommentsByPostId(String postId) {
        List<Comment> comments = new ArrayList<>();
        try {
            DocumentReference postRef = db.collection("posts").document(postId);
            CollectionReference commentsRef = postRef.collection("comments");
            ApiFuture<QuerySnapshot> future = commentsRef.get();

            for (DocumentSnapshot document : future.get().getDocuments()) {
                Comment comment = document.toObject(Comment.class);
                comment.setCommentId(document.getId());
                comments.add(comment);
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return comments;
    }
}
