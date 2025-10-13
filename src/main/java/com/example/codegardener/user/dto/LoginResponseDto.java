package com.example.codegardener.user.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor // 모든 필드를 인자로 받는 생성자를 추가합니다.
public class LoginResponseDto {
    private String accessToken;
}