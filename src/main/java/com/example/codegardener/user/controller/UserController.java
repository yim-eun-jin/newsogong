package com.example.codegardener.user.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.RequiredArgsConstructor;

import com.example.codegardener.user.dto.LoginRequestDto;
import com.example.codegardener.user.dto.LoginResponseDto;
import com.example.codegardener.user.dto.SignUpRequestDto;
import com.example.codegardener.user.dto.UserResponseDto;
import com.example.codegardener.user.service.UserService;

@Slf4j
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

    // 출석 체크
    @PostMapping("/attendance")
    public ResponseEntity<String> checkAttendance(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
        }

        try {
            String message = userService.recordAttendance(userDetails.getUsername());
            return ResponseEntity.ok(message);
        } catch (IllegalArgumentException e) { // 사용자를 찾을 수 없는 경우 등
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        } catch (Exception e) { // 그 외 서버 오류
            log.error("Error recording attendance for user: {}", userDetails.getUsername(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("출석 처리 중 오류가 발생했습니다.");
        }
    }


    @DeleteMapping("/me")
    public ResponseEntity<String> deleteMyAccount(
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("인증이 필요합니다.");
        }

        try {
            userService.deleteCurrentUser(userDetails.getUsername());
            // TODO: 탈퇴 성공 후 클라이언트에게 JWT 토큰을 삭제하도록 안내 필요 (예: 응답 헤더나 본문에 명시)
            return ResponseEntity.ok("회원 탈퇴가 완료되었습니다.");
        } catch (IllegalStateException e) { // 관리자 탈퇴 시도 등
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) { // 그 외 예외 처리
            log.error("Error deleting user account: {}", userDetails.getUsername(), e); // 로그 추가
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("회원 탈퇴 중 오류가 발생했습니다.");
        }
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