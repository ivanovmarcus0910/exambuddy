package com.example.exambuddy.controller;

import com.example.exambuddy.service.CookieService;
import com.example.exambuddy.service.FirebaseAuthService;
import com.example.exambuddy.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;

@Component
public class AutoLoginFilter extends OncePerRequestFilter {

    @Autowired
    private UserService userService;
    @Autowired
    private CookieService cookieService;
    @Autowired
    private FirebaseAuthService authService;
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
    {
    try {
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("loggedInUser") == null) {

            String username = cookieService.getCookie(request, "rememberedUsername");
            String password = cookieService.getCookie(request, "rememberedPassword");
            if (username != null && password != null) {
                if (authService.authenticate(URLDecoder.decode(username,  "UTF-8") , URLDecoder.decode(password, "UTF-8"))) {
                    cookieService.setCookie(response, "noname", URLEncoder.encode(username, "UTF-8"));
                    session = request.getSession(true);
                    session.setAttribute("loggedInUser", username);
                    session.setAttribute("urlimg", UserService.getAvatarUrlByUsername(username));
                }
            }

        }
        filterChain.doFilter(request, response);

    } catch (Exception e) {
        System.out.println("Lỗi mẹ rồi");
        e.printStackTrace();
    }

    }
}
