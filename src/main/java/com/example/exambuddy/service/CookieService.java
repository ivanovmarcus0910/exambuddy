package com.example.exambuddy.service;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;

@Service
public class CookieService {
    public void setCookie(HttpServletResponse response, String name, String value) {
        Cookie cookie = new Cookie(name, value);
        cookie.setPath("/"); // Áp dụng cho toàn bộ domain
        cookie.setHttpOnly(false);
        cookie.setSecure(false);
        cookie.setMaxAge(24 * 60 * 60); // Thời gian sống (giây)
        response.addCookie(cookie);
    }
    public String getCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {
            if (name.equals(cookie.getName()))
                return cookie.getValue();
        }
        return null;
    }
    public void removeCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setMaxAge(0); // Xóa cookie bằng cách đặt thời gian sống = 0
        cookie.setPath("/");
        response.addCookie(cookie);
    }
}
