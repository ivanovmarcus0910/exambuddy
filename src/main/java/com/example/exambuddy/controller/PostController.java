package com.example.exambuddy.controller;

import com.example.exambuddy.model.Comment;
import com.example.exambuddy.model.Post;
import com.example.exambuddy.service.CloudinaryService;
import com.example.exambuddy.service.CookieService;
import com.example.exambuddy.service.PostService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.text.SimpleDateFormat;
import java.util.*;

@Controller
@RequestMapping("/forum")
public class PostController {
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private CookieService cookieService;
    @Autowired
    private PostService postService;

    @PostMapping("/create")
    public String createPost(@RequestParam String content,
                             @RequestParam("images") MultipartFile[] files,
                             HttpServletRequest request,
                             Model model) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("loggedInUser");

        if (username == null) {
            System.out.println("❌ Lỗi: User chưa đăng nhập!");
            return "redirect:/login";
        }

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String imageUrl = this.cloudinaryService.upLoadImg(file, "imgForum");
                imageUrls.add(imageUrl);
                System.out.println("URL = " + imageUrl);
            }
        }

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String date = formatter.format(new Date());

        Post post = PostService.savePost(username, content, date, imageUrls);
        model.addAttribute("post", post);
        return "redirect:/forum";
    }

    @GetMapping
    public String getForum(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("loggedInUser");

        List<Post> posts = postService.getPostsFromFirestore();
        for (Post post : posts) {
            // Lấy danh sách bình luận của bài viết
            List<Comment> comments = PostService.getCommentsByPostId(post.getPostId());
            post.setComments(comments != null ? comments : new ArrayList<>());

            // Kiểm tra xem username hiện tại đã like bài viết hay chưa
            post.setLiked(post.getLikedUsernames() != null && post.getLikedUsernames().contains(username));
        }

        model.addAttribute("posts", posts);
        model.addAttribute("username", username);
        return "forum";
    }

    @PostMapping("/comment")
    public String createComment(@RequestParam String postId,
                                @RequestParam String content,
                                @RequestParam("commentImages") MultipartFile[] files,
                                @RequestParam String username,
                                Model model) {
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            if (!file.isEmpty()) {
                String imageUrl = this.cloudinaryService.upLoadImg(file, "imgForum");
                imageUrls.add(imageUrl);
                System.out.println("URL = " + imageUrl);
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