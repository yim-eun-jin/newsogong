package com.example.codegardener.user.service;

import com.example.codegardener.user.dto.LoginRequestDto;
import com.example.codegardener.user.dto.SignUpRequestDto;
import com.example.codegardener.user.dto.UserResponseDto;
import com.example.codegardener.user.entity.User;
import com.example.codegardener.user.jwt.JwtUtil;
import com.example.codegardener.user.repository.UserRepository;
import com.example.codegardener.user.role.Role;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    // 회원 가입
    @Transactional
    public UserResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        // 1. 아이디 중복 확인
        if (userRepository.findByUserName(signUpRequestDto.getUserName()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        // 2. 이메일 중복 확인
        if (userRepository.findByEmail(signUpRequestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        // 3. 사용자 생성 및 저장
        User newUser = new User();
        newUser.setUserName(signUpRequestDto.getUserName());
        newUser.setEmail(signUpRequestDto.getEmail());

        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());
        newUser.setPassword(encodedPassword);

        newUser.setRole(Role.USER);

        User savedUser = userRepository.save(newUser);

        // 4. 응답 DTO로 변환하여 반환
        return new UserResponseDto(savedUser);
    }

    // 로그인
    @Transactional(readOnly = true) // 읽기 전용으로 성능 최적화
    public String login(LoginRequestDto loginRequestDto) {
        // 1. 아이디로 사용자 조회
        User user = userRepository.findByUserName(loginRequestDto.getUserName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        // 2. 비밀번호 일치 여부 확인
        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        // 3. 로그인 성공 시, JWT 생성하여 반환
        return jwtUtil.createToken(user.getUserName(), user.getRole());
    }

    // 사용자 조회
    @Transactional(readOnly = true)
    public UserResponseDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));

        return new UserResponseDto(user);
    }
}