package com.example.exambuddy.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.disable()) // Tắt CORS của Spring Security
                .csrf(csrf -> csrf.disable()) // Tắt CSRF để tránh lỗi Forbidden
                .authorizeHttpRequests(auth -> auth
                                //.requestMatchers("/api/users/register","/api/users/login","/api/users/verify").permitAll() // Cho phép truy cập tất cả API
                                //.requestMatchers("/signup").permitAll()
//                        .requestMatchers("/admin/**").hasRole("ADMIN")
//                        .requestMatchers("/user/**").hasAnyRole("USER", "UPGRADED_USER")
//                        .requestMatchers("/teacher/**").hasRole("TEACHER")
//                        .requestMatchers("/upgraded/**").hasRole("UPGRADED_USER")
                                .anyRequest().permitAll() // Cho phép tất cả request khác
                )
                .oauth2Login(oauth2 -> oauth2
                        .loginPage("/login")
                        .defaultSuccessUrl("/oauth2/success", true)
                        .failureUrl("/oauth2/failure")
                )
                .logout(logout -> logout
                        .logoutUrl("/logout") // Đường dẫn logout
                        .deleteCookies("JSESSIONID")
                        .deleteCookies("noname")
                        .deleteCookies("rememberedUsername")
                        .deleteCookies("rememberedPassword")
                        .invalidateHttpSession(true)
                        .logoutSuccessUrl("/home") // Chuyển hướng sau khi logout
                );


        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}