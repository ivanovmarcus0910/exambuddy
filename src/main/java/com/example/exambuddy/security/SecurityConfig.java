package com.example.exambuddy.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configure(http)) // Bật CORS, cần cấu hình thêm
                .csrf(csrf -> csrf.disable()) // Tắt CSRF nếu chỉ dùng API
                //.cors(cors -> cors.disable()) // Tắt CORS của Spring Security
                //.csrf(csrf -> csrf.disable()) // Tắt CSRF để tránh lỗi Forbidden
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/users/register","/api/users/login","/api/users/verify").permitAll() // Cho phép truy cập tất cả API
                        .anyRequest().permitAll() // Cho phép tất cả request khác
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Không lưu session
                .httpBasic(withDefaults()); // Dùng Basic Authentication

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
