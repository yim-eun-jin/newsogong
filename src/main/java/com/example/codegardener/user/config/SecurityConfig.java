package com.example.codegardener.user.config;

import com.example.codegardener.user.jwt.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable()) // CSRF 보호 비활성화
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // 세션 관리 비활성화 (Stateless)
                .authorizeHttpRequests(auth -> auth
                        // 수정해야함
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/api/user/signup",
                                "/api/user/login",
                                "/api/user/test"
                        ).permitAll()

                        // 1. 로그인, 회원가입은 항상 허용
                        .requestMatchers("/api/user/signup", "/api/user/login").permitAll()
                        // 2. GET 요청은 대부분 허용 (게시물 조회, 사용자 프로필 조회 등)
                        .requestMatchers(HttpMethod.GET, "/api/**").permitAll()
                        // 3. 그 외 나머지 요청(POST, PUT, DELETE 등)은 인증 필요
                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // 기본 필터 앞에 추가

        return http.build();
    }
}