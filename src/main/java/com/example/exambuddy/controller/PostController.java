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
import org.springframework.web.bind.annotation.RequestParam;

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
    public String getPostDetail(@PathVariable String postId,
                                @RequestParam(value = "modal", required = false, defaultValue = "false") boolean isModal,
                                Model model,
                                HttpServletRequest request) {
        HttpSession session = request.getSession();
        String username = (String) session.getAttribute("loggedInUser");

        // Lấy bài viết theo postId
        Post post = postService.getPostById(postId);

        // Kiểm tra xem user đã like post hay chưa
        post.setLiked(post.getLikedUsernames() != null && post.getLikedUsernames().contains(username));

        // Lấy tất cả bình luận của bài viết (ĐÃ SỬA LỖI: Thêm `username` vào)
        List<Comment> comments = postService.getCommentsByPostId(post.getPostId(), username);
        post.setComments(comments != null ? comments : new ArrayList<>());

        // Lấy avatar của user hiện tại
        String avatarUrl = UserService.getAvatarUrlByUsername(username);

        // Đưa dữ liệu vào model để hiển thị trên giao diện
        model.addAttribute("post", post);
        model.addAttribute("username", username);
        model.addAttribute("avatarUrl", avatarUrl);

        // Nếu là modal thì chỉ trả về nội dung bài viết
        if (isModal) {
            return "fragments/post"; // Trả về fragment mà không có header & aside
        }

        return "postDetail"; // Trả về trang đầy đủ
    }

}
