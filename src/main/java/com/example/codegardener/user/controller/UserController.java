package com.example.codegardener.user.controller;

import com.example.codegardener.user.dto.LoginRequestDto;
import com.example.codegardener.user.dto.LoginResponseDto;
import com.example.codegardener.user.dto.SignUpRequestDto;
import com.example.codegardener.user.dto.UserResponseDto;
import com.example.codegardener.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/user")
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ResponseEntity<UserResponseDto> signUp(@RequestBody SignUpRequestDto signUpRequestDto) {
        UserResponseDto userResponseDto = userService.signUp(signUpRequestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(userResponseDto);
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
        String token = userService.login(loginRequestDto);
        return ResponseEntity.ok(new LoginResponseDto(token));
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserResponseDto> getUserProfile(@PathVariable Long userId) {
        UserResponseDto userResponseDto = userService.getUserProfile(userId);
        return ResponseEntity.ok(userResponseDto);
    }
}