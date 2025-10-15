package com.example.codegardener.user.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignUpRequestDto {
    private String userName;
    private String email;
    private String password;
}