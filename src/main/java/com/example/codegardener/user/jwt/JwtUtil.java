package com.example.codegardener.user.jwt;

import com.example.codegardener.user.role.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    private final SecretKey secretKey;
    private final long expirationTime;

    public JwtUtil(@Value("${jwt.secret.key}") String secretKey,
                   @Value("${jwt.expiration.time}") long expirationTime) {
        this.secretKey = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        this.expirationTime = expirationTime;
    }

    // JWT 생성 메서드
    public String createToken(String username, Role role) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expirationTime);

        return Jwts.builder()
                .subject(username) // 토큰의 주체 (사용자 이름)
                .claim("role", role.name()) // 사용자 역할
                .issuedAt(now) // 토큰 발급 시간
                .expiration(expiryDate) // 토큰 만료 시간
                .signWith(secretKey) // 비밀 키로 서명
                .compact();
    }

    // JWT 검증 및 정보 추출 메서드
    public Claims getClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 사용자 이름 추출
    public String getUsername(String token) {
        return getClaims(token).getSubject();
    }

    // 토큰 만료 여부 확인
    public boolean isTokenExpired(String token) {
        return getClaims(token).getExpiration().before(new Date());
    }
}