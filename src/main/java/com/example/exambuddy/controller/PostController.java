package com.example.exambuddy.controller;

import com.example.exambuddy.model.Comment;
import com.example.exambuddy.model.Post;
import com.example.exambuddy.service.CloudinaryService;
import com.example.exambuddy.service.PostService;
import com.example.exambuddy.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.ArrayList;
import java.util.List;

@Controller
@RequestMapping("/postDetail")
public class PostController {
    @Autowired
    private CloudinaryService cloudinaryService;
    @Autowired
    private PostService postService;

    @GetMapping("/{postId}")
    public String getPostDetail(@PathVariable String postId, Model model, HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("loggedInUser");

        // Lấy bài viết theo postId
        Post post = postService.getPostById(postId);

        // Lấy tất cả bình luận của bài viết
        List<Comment> comments = PostService.getCommentsByPostId(post.getPostId());
        if (comments != null) {
            for (Comment comment : comments) {
                String avatarUrl = UserService.getAvatarUrlByUsername(comment.getUsername());
                comment.setAvatarUrl(avatarUrl);
            }
        }
        post.setComments(comments != null ? comments : new ArrayList<>());
        post.setLiked(post.getLikedUsernames() != null && post.getLikedUsernames().contains(username));

        String avatarUrl = UserService.getAvatarUrlByUsername(username);

        model.addAttribute("post", post);
        model.addAttribute("username", username);
        model.addAttribute("avatarUrl", avatarUrl);

        return "postDetail"; // Hiển thị trang postDetail
    }
}
