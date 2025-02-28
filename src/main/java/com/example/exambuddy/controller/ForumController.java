package com.example.exambuddy.controller;

import com.example.exambuddy.model.Comment;
import com.example.exambuddy.model.Post;
import com.example.exambuddy.service.CloudinaryService;
import com.example.exambuddy.service.CookieService;
import com.example.exambuddy.service.PostService;
import com.example.exambuddy.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
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
    private CookieService cookieService;
    @Autowired
    private PostService postService;

    @PostMapping("/create")
    public String createPost(@RequestParam String content,
                             @RequestParam("subject") String subject,
                             @RequestParam(value = "grade", required = false, defaultValue = "Chung") String grade,
                             @RequestParam("images") MultipartFile[] files,
                             HttpServletRequest request,
                             Model model) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "redirect:/login";
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String imageUrl = this.cloudinaryService.upLoadImg(file, "imgForum");
                imageUrls.add(imageUrl);
            }
        }
        subject = subject.trim().replace(",", "");

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = formatter.format(new Date());

        // Lưu bài viết
        Post post = PostService.savePost(username, content, subject, grade, date, imageUrls);
        model.addAttribute("post", post);

        return "redirect:/forum?subject=" + subject;
    }

    @GetMapping
    public String getForum(@RequestParam(value = "subject", required = false) String subject,
                           @RequestParam(value = "grade", required = false) String grade,
                           Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("loggedInUser");

        // Lấy tất cả bài viết ban đầu (trang forum chung)
        List<Post> posts = postService.getPostsFromFirestore();

        // Nếu có subject, lọc bài viết theo môn học
        if (subject != null && !subject.isEmpty()) {
            posts = posts.stream()
                    .filter(post -> subject.trim().replace(",", "").equalsIgnoreCase(post.getSubject()))
                    .collect(Collectors.toList());
        }

        // Nếu có chọn lớp học, lọc tiếp
        if (grade != null && !grade.isEmpty()) {
            posts = posts.stream()
                    .filter(post -> grade.equals(post.getGrade()))
                    .collect(Collectors.toList());
        }

        for (Post post : posts) {
            List<Comment> comments = PostService.getCommentsByPostId(post.getPostId());
            if (comments != null) {
                for (Comment comment : comments) {
                    String avatarUrl = UserService.getAvatarUrlByUsername(comment.getUsername());
                    comment.setAvatarUrl(avatarUrl);
                }
            }
            post.setComments(comments != null ? comments : new ArrayList<>());
            post.setLiked(post.getLikedUsernames() != null && post.getLikedUsernames().contains(username));
        }

        String avatarUrl = UserService.getAvatarUrlByUsername(username);

        model.addAttribute("posts", posts);
        model.addAttribute("username", username);
        model.addAttribute("avatarUrl", avatarUrl);
        model.addAttribute("selectedSubject", subject);
        model.addAttribute("selectedGrade", grade);

        return "forum"; // Hiển thị trang forum chung nếu không có subject
    }

    @PostMapping("/comment")
    public String createComment(@RequestParam String postId,
                                @RequestParam String content,
                                @RequestParam("commentImages") MultipartFile[] files,
                                HttpServletRequest request,
                                Model model) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            return "redirect:/login";
        }

        List<String> imageUrls = new ArrayList<>();

        if (files != null) { // Kiểm tra nếu có file mới xử lý
            for (MultipartFile file : files) {
                if (!file.isEmpty()) {
                    String imageUrl = this.cloudinaryService.upLoadImg(file, "imgForum");
                    imageUrls.add(imageUrl);
                    System.out.println("URL = " + imageUrl);
                }
            }
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = formatter.format(new Date());

        Comment comment = PostService.saveComment(postId, username, content, date, imageUrls);
        model.addAttribute("comment", comment);

        return "redirect:/forum";
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
}