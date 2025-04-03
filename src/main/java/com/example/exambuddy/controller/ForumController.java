package com.example.exambuddy.controller;

import com.example.exambuddy.model.Comment;
import com.example.exambuddy.model.Notification;
import com.example.exambuddy.model.Post;
import com.example.exambuddy.service.CloudinaryService;
import com.example.exambuddy.service.CookieService;
import com.example.exambuddy.service.PostService;
import com.example.exambuddy.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/forum")
public class ForumController {
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private PostService postService;

    @PostMapping("/create")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> createPost(@RequestParam String content,
                                                          @RequestParam("subject") String subject,
                                                          @RequestParam(value = "grade", required = false, defaultValue = "Chung") String grade,
                                                          @RequestParam("images") MultipartFile[] files,
                                                          @RequestParam("status") String status, HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", "Chưa đăng nhập");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }

        String avatarUrl = UserService.getAvatarUrlByUsername(username);
        List<String> imageUrls = new ArrayList<>();

        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String imageUrl = this.cloudinaryService.upLoadImg(file, "imgForum/imgPosts");
                imageUrls.add(imageUrl);
            }
        }

        subject = subject.trim().replace(",", "");
        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = formatter.format(new Date());

        // Lưu bài viết
        Post post = PostService.savePost(username, avatarUrl, content, subject, grade, date, status, imageUrls);

        // Tạo response JSON
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("postId", post.getPostId());
        response.put("username", username);
        response.put("avatarUrl", avatarUrl);
        response.put("content", content);
        response.put("subject", subject);
        response.put("grade", grade);
        response.put("timeAgo", "Vừa xong"); // Có thể cần tính toán thời gian thực
        response.put("imageUrls", imageUrls);
        response.put("likeCount", 0);
        response.put("liked", false);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public String forumPage(HttpSession session, Model model) {
        String username = (String) session.getAttribute("loggedInUser");

        String avatarUrl = UserService.getAvatarUrlByUsername(username);

        model.addAttribute("username", username);
        model.addAttribute("avatarUrl", avatarUrl);
        return "forum";
    }

    @GetMapping("/posts")
    public ResponseEntity<List<Map<String, Object>>> getPosts(
            HttpSession session,
            @RequestParam(value = "subject", required = false) String subject,
            @RequestParam(value = "grade", required = false) String grade) {

        String username = (String) session.getAttribute("loggedInUser");
        List<Post> posts = postService.getPublicPostsFromFirestore();

        if (subject != null && !subject.isEmpty()) {
            posts = posts.stream()
                    .filter(post -> subject.trim().replace(",", "").equalsIgnoreCase(post.getSubject()))
                    .collect(Collectors.toList());
        }

        if (grade != null && !grade.isEmpty()) {
            posts = posts.stream()
                    .filter(post -> grade.equals(post.getGrade()))
                    .collect(Collectors.toList());
        }

        // Chuyển đổi danh sách bài viết thành danh sách Map (JSON)
        List<Map<String, Object>> postList = posts.stream().map(post -> {
            Map<String, Object> map = new HashMap<>();
            map.put("postId", post.getPostId());
            map.put("avatarUrl", post.getAvatarUrl());
            map.put("username", post.getUsername());
            map.put("timeAgo", post.getTimeAgo());
            map.put("content", post.getContent());
            map.put("subject", post.getSubject());
            map.put("grade", post.getGrade());
            map.put("likeCount", post.getLikeCount());
            map.put("liked", post.isLiked());
            map.put("imageUrls", post.getImageUrls()); // Trả về danh sách ảnh

            // Lấy số lượng bình luận
//            int commentCount = postService.getCommentsByPostId(post.getPostId(), username).size();
//            post.setCommentCount(commentCount);
//            map.put("commentCount", post.getCommentCount());

            return map;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(postList);
    }

    @GetMapping("/notifications")
    public ResponseEntity<List<Map<String, Object>>> getNotifications(HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");
        List<Notification> notifications = postService.getUserNotifications(username);
        List<Map<String, Object>> notificationMaps = new ArrayList<>();

        for (Notification noti : notifications) {
            Map<String, Object> notiMap = new HashMap<>();
            notiMap.put("notificationId", noti.getNotificationId());
            notiMap.put("postId", noti.getPostId());
            notiMap.put("sender", noti.getSender());
            notiMap.put("receiver", noti.getReceiver());
            notiMap.put("content", noti.getContent());
            notiMap.put("date", noti.getDate());
            notiMap.put("type", noti.getType());
            notiMap.put("timeAgo", noti.getTimeAgo());
            notiMap.put("isRead", noti.isRead());
            notificationMaps.add(notiMap);
        }

        System.out.println("📌 Notifications gửi về frontend:");
        for (Map<String, Object> noti : notificationMaps) {
            System.out.println(noti);
        }

        return ResponseEntity.ok(notificationMaps);
    }

    @PostMapping("/markAsRead/{postId}")
    @ResponseBody
    public ResponseEntity<String> markAsRead(@PathVariable String postId, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not logged in");
        }

        String result = postService.markNotificationAsRead(username, postId);

        if ("Updated".equals(result)) {
            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @PostMapping("/comment")
    @ResponseBody
    public ResponseEntity<?> createComment(
            @RequestParam String postId,
            @RequestParam(required = false) String parentCommentId,
            @RequestParam String content,
            @RequestPart(value = "commentImages", required = false) MultipartFile[] files,
            HttpSession session) {

        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để bình luận.");
        }

        List<String> imageUrls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String imageUrl = this.cloudinaryService.upLoadImg(file, "imgForum/imgComments");
                    imageUrls.add(imageUrl);
                }
            }
        }

        String avatarUrl = UserService.getAvatarUrlByUsername(username);
        String date = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());

        Comment comment = PostService.saveComment(postId, parentCommentId, username, avatarUrl, content, date, imageUrls);

        return ResponseEntity.ok(comment);
    }


    @PostMapping("/like")
    @ResponseBody
    public Map<String, Object> likePost(@RequestParam String postId,
                                        @RequestParam boolean liked,
                                        @RequestParam String username) {
        // Cập nhật số lượt thích
        postService.updateLikeCount(postId, username, liked);

        // Lấy lại bài đăng để lấy số like mới nhất
        Post updatedPost = postService.getPostById(postId);

        // Tạo phản hồi JSON
        Map<String, Object> response = new HashMap<>();
        if (updatedPost != null) {
            response.put("likeCount", updatedPost.getLikeCount());
        } else {
            response.put("likeCount", 0);
        }
        return response;
    }

    @PostMapping("/edit")
    @ResponseBody
    public String updatePost(@RequestParam String postId,
                             @RequestParam String content,
                             @RequestParam("images") MultipartFile[] files,
                             HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "Bạn cần đăng nhập để chỉnh sửa bài viết";
        }
        // Lấy danh sách ảnh mới
        List<String> imageUrls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String imageUrl = cloudinaryService.upLoadImg(file, "imgForum/imgPosts");
                    imageUrls.add(imageUrl);
                }
            }
        }

        postService.updatePost(postId, username, content, imageUrls);
        return "success";
    }

    @DeleteMapping("/delete/{postId}")
    @ResponseBody
    public String deletePost(@PathVariable String postId, HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "Bạn cần đăng nhập để xóa bài viết";
        }

        boolean success = PostService.deletePost(postId);
        return success ? "success" : "fail";
    }

    @PostMapping("/likeComment")
    @ResponseBody
    public Map<String, Object> likeComment(@RequestParam String postId,
                                           @RequestParam String commentId,
                                           @RequestParam boolean liked,
                                           @RequestParam String username) {
        // Cập nhật số lượt thích
        postService.updateCommentLikeCount(postId, commentId, username, liked);

        // Lấy lại bình luận để lấy số like mới nhất
        Comment updatedComment = postService.getCommentById(postId, commentId);

        // Tạo phản hồi JSON
        Map<String, Object> response = new HashMap<>();
        if (updatedComment != null) {
            response.put("likeCount", updatedComment.getLikeCount());
        } else {
            response.put("likeCount", 0);
        }
        return response;
    }

    @PostMapping("/comment/edit")
    @ResponseBody
    public ResponseEntity<?> editComment(
            @RequestParam String commentId,
            @RequestParam String postId,
            @RequestParam String content,
            @RequestParam(required = false, defaultValue = "true") boolean keepOldImages,
            @RequestPart(value = "newImages", required = false) MultipartFile[] files,
            HttpSession session) {

        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Bạn cần đăng nhập để chỉnh sửa bình luận.");
        }

        List<String> imageUrls = new ArrayList<>();
        if (!keepOldImages && files != null) { // Nếu không giữ ảnh cũ và có ảnh mới
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String imageUrl = cloudinaryService.upLoadImg(file, "imgForum/imgComments");
                    imageUrls.add(imageUrl);
                }
            }
        }

        boolean success = postService.updateComment(postId, commentId, username, content, imageUrls, keepOldImages);
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "commentId", commentId, "content", content, "imageUrls", imageUrls));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", "Không thể cập nhật bình luận."));
        }
    }

    @PostMapping("/comment/delete")
    public ResponseEntity<?> deleteComment(@RequestBody Map<String, String> requestData, HttpSession session) {
        String username = (String) session.getAttribute("loggedInUser");
        if (username == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false, "message", "Bạn cần đăng nhập để thực hiện thao tác này."));
        }

        String postId = requestData.get("postId");
        String commentId = requestData.get("commentId");

        boolean deleted = postService.deleteComment(postId, commentId, username);
        if (deleted) {
            return ResponseEntity.ok(Map.of("success", true));
        } else {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("success", false, "message", "Không thể xóa bình luận!"));
        }
    }

}