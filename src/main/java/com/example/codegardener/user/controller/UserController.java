package com.example.codegardener.user.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.RequiredArgsConstructor;

import com.example.codegardener.user.dto.LoginRequestDto;
import com.example.codegardener.user.dto.LoginResponseDto;
import com.example.codegardener.user.dto.SignUpRequestDto;
import com.example.codegardener.user.dto.UserResponseDto;
import com.example.codegardener.user.service.UserService;

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

    // ===== 관리자용 API =====

    @DeleteMapping("/{userId}/admin")
    public ResponseEntity<String> deleteUserByAdmin(
            @PathVariable Long userId,
            @AuthenticationPrincipal UserDetails adminUserDetails
    ) {
        if (adminUserDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("관리자 인증이 필요합니다.");
        }

        try {
            userService.deleteUserByAdmin(userId, adminUserDetails.getUsername());
            return ResponseEntity.ok("사용자(ID: " + userId + ")가 삭제되었습니다.");
        } catch (AccessDeniedException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("사용자 삭제 중 오류 발생");
        }
    }
}