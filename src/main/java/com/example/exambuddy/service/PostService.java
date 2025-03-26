package com.example.exambuddy.service;

import com.example.exambuddy.model.Post;
import com.example.exambuddy.model.Comment;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class PostService {
    private static final Firestore db = FirestoreClient.getFirestore();
    private static final String COLLECTION_NAME = "posts";

    public static Post savePost(String username, String avatarUrl, String content, String subject, String grade, String date, String status, List<String> imageUrls) {
        CollectionReference posts = db.collection(COLLECTION_NAME);

        Post post = new Post();
        post.setUsername(username);
        post.setAvatarUrl(avatarUrl);
        post.setContent(content);
        post.setSubject(subject);
        post.setGrade(grade);
        post.setDate(date);
        post.setStatus(status);
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

    public static List<Post> getPublicPostsFromFirestore() {
        List<Post> postList = new ArrayList<>();

        try {
            // Truy vấn chỉ lấy bài có status = "public"
            ApiFuture<QuerySnapshot> future = db.collection(COLLECTION_NAME)
                    .whereEqualTo("status", "public")  // Lọc bài viết public
                    .get();

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


    public static Comment saveComment(String postId, String parentCommentId, String username, String avatarUrl, String content, String date, List<String> imageUrls) {

        DocumentReference postRef = db.collection("posts").document(postId);
        CollectionReference commentsRef = postRef.collection("comments");

        Comment comment = new Comment();
        comment.setPostId(postId);
        comment.setParentCommentId(parentCommentId);
        comment.setUsername(username);
        comment.setAvatarUrl(avatarUrl);
        comment.setContent(content);
        comment.setDate(date);
        comment.setImageUrls(imageUrls);

        try {
            DocumentReference newCommentRef = commentsRef.add(comment).get();
            comment.setCommentId(newCommentRef.getId());
            newCommentRef.update("commentId", comment.getCommentId()).get();
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
            DocumentReference postRef = db.collection("posts").document(postId);
            CollectionReference commentsRef = postRef.collection("comments");
            ApiFuture<QuerySnapshot> future = commentsRef.get();

            // Tạo map để nhóm phản hồi theo comment cha
            Map<String, List<Comment>> replyMap = new HashMap<>();

            for (DocumentSnapshot document : future.get().getDocuments()) {
                Comment comment = document.toObject(Comment.class);
                comment.setCommentId(document.getId());

                // Kiểm tra user đã like chưa
                comment.setLiked(comment.getLikedUsernames() != null && comment.getLikedUsernames().contains(username));

                // Phân loại comment gốc & reply
                if (comment.getParentCommentId() == null || "null".equals(comment.getParentCommentId()) || comment.getParentCommentId().isEmpty()) {
                    comments.add(comment); // Đây là bình luận chính
                } else {
                    replyMap.computeIfAbsent(comment.getParentCommentId(), k -> new ArrayList<>()).add(comment);
                }
            }

            // Gán danh sách reply chỉ 1 cấp vào từng comment cha (bỏ đệ quy)
            for (Comment comment : comments) {
                comment.setReplies(replyMap.getOrDefault(comment.getCommentId(), new ArrayList<>()));
            }
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

    public List<Comment> getUserLatestComments(String username) {
        Map<String, Comment> latestCommentsMap = new HashMap<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        try {
            CollectionReference postsRef = db.collection("posts");
            ApiFuture<QuerySnapshot> future = postsRef.get();

            for (QueryDocumentSnapshot postDoc : future.get().getDocuments()) {
                String postId = postDoc.getId();
                String postOwner = postDoc.getString("username"); // Lấy username của chủ bài post
                CollectionReference commentsRef = postDoc.getReference().collection("comments");

                Query query = commentsRef.whereEqualTo("username", username);
                ApiFuture<QuerySnapshot> commentFuture = query.get();

                Comment latestComment = null;

                for (DocumentSnapshot commentDoc : commentFuture.get().getDocuments()) {
                    Comment comment = commentDoc.toObject(Comment.class);
                    comment.setCommentId(commentDoc.getId());
                    comment.setPostId(postId);
                    comment.setPostOwner(postOwner); // Lưu username của chủ post

                    if (comment.getDate() != null && !comment.getDate().isEmpty()) {
                        comment.setLocalDateTime(LocalDateTime.parse(comment.getDate(), formatter));
                    } else {
                        comment.setLocalDateTime(LocalDateTime.MIN);
                    }

                    // Lấy comment mới nhất của mỗi post
                    if (latestComment == null || comment.getLocalDateTime().isAfter(latestComment.getLocalDateTime())) {
                        latestComment = comment;
                    }
                }

                if (latestComment != null) {
                    latestCommentsMap.put(postId, latestComment);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        // Chuyển Map thành List và sắp xếp theo thời gian mới nhất
        List<Comment> latestComments = new ArrayList<>(latestCommentsMap.values());
        latestComments.sort((c1, c2) -> c2.getLocalDateTime().compareTo(c1.getLocalDateTime()));

        return latestComments;
    }

    public static List<Post> getPostsByUsername(String username) {
        List<Post> userPosts = new ArrayList<>();
        try {
            // Truy vấn Firestore, lấy các post có trường "username" == username
            Query query = db.collection(COLLECTION_NAME)
                    .whereEqualTo("username", username);
            List<QueryDocumentSnapshot> documents = query.get().get().getDocuments();

            for (DocumentSnapshot doc : documents) {
                Post post = doc.toObject(Post.class);
                if (post != null) {
                    post.setPostId(doc.getId()); // gán ID tài liệu cho post
                    userPosts.add(post);
                }
            }
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
        return userPosts;
    }

    public boolean updateComment(String postId, String commentId, String username, String newContent, List<String> newImageUrls, boolean keepOldImages) {
        try {
            DocumentReference commentRef = db.collection("posts")
                    .document(postId)
                    .collection("comments")
                    .document(commentId);

            DocumentSnapshot document = commentRef.get().get();
            if (!document.exists()) {
                System.out.println("❌ Không tìm thấy bình luận với ID: " + commentId);
                return false;
            }

            Comment comment = document.toObject(Comment.class);
            if (comment == null || !comment.getUsername().equals(username)) {
                System.out.println("❌ Không có quyền chỉnh sửa bình luận này");
                return false;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("content", newContent);

            if (!keepOldImages) {
                updates.put("imageUrls", newImageUrls);
            }

            commentRef.update(updates).get();
            System.out.println("✅ Cập nhật bình luận thành công: " + commentId);
            return true;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Lỗi khi cập nhật bình luận: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteComment(String postId, String commentId, String username) {
        DocumentReference postRef = db.collection("posts").document(postId);
        CollectionReference commentsRef = postRef.collection("comments");
        DocumentReference commentRef = commentsRef.document(commentId);

        try {
            DocumentSnapshot commentDoc = commentRef.get().get();
            if (!commentDoc.exists()) {
                System.out.println("Lỗi không thấy commentId: " + commentId);
                return false; // Bình luận không tồn tại
            }

            Comment comment = commentDoc.toObject(Comment.class);
            if (comment == null || !comment.getUsername().equals(username)) {
                System.out.println("Lỗi rồi");
                return false; // Không có quyền xóa bình luận
            }

            commentRef.delete().get(); // Xóa bình luận trong Firestore
            return true;
        } catch (InterruptedException | ExecutionException e) {
            System.err.println("❌ Lỗi khi xóa bình luận: " + e.getMessage());
            return false;
        }
    }
}