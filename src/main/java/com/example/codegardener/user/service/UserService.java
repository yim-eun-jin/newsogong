package com.example.codegardener.user.service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

import com.example.codegardener.feedback.repository.FeedbackRepository;
import com.example.codegardener.global.jwt.JwtUtil;
import com.example.codegardener.user.domain.Role;
import com.example.codegardener.user.domain.User;
import com.example.codegardener.user.dto.LoginRequestDto;
import com.example.codegardener.user.dto.SignUpRequestDto;
import com.example.codegardener.user.dto.UserResponseDto;
import com.example.codegardener.user.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FeedbackRepository feedbackRepository;

    // 회원 가입
    @Transactional
    public UserResponseDto signUp(SignUpRequestDto signUpRequestDto) {
        if (userRepository.findByUserName(signUpRequestDto.getUserName()).isPresent()) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        if (userRepository.findByEmail(signUpRequestDto.getEmail()).isPresent()) {
            throw new IllegalArgumentException("이미 등록된 이메일입니다.");
        }

        User newUser = new User();
        newUser.setUserName(signUpRequestDto.getUserName());
        newUser.setEmail(signUpRequestDto.getEmail());
        String encodedPassword = passwordEncoder.encode(signUpRequestDto.getPassword());
        newUser.setPassword(encodedPassword);
        newUser.setRole(Role.USER);

        User savedUser = userRepository.save(newUser);
        return new UserResponseDto(savedUser);
    }

    // 로그인
    @Transactional(readOnly = true)
    public String login(LoginRequestDto loginRequestDto) {
        User user = userRepository.findByUserName(loginRequestDto.getUserName())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 아이디입니다."));

        if (!passwordEncoder.matches(loginRequestDto.getPassword(), user.getPassword())) {
            throw new IllegalArgumentException("비밀번호가 틀렸습니다.");
        }

        return jwtUtil.createToken(user.getUserName(), user.getRole());
    }

    // 사용자 프로필 조회
    @Transactional(readOnly = true)
    public UserResponseDto getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. ID: " + userId));
        return new UserResponseDto(user);
    }

    // ===== 리더보드 기능 =====

    // 누적 포인트 TOP3
    public List<UserResponseDto> getTop3LeaderboardByPoints() {
        return userRepository.findTop3ByOrderByUserProfile_PointsDesc().stream()
                .map(UserResponseDto::new).collect(Collectors.toList());
    }
    // 누적 포인트 페이징
    public Page<UserResponseDto> getLeaderboardByPoints(Pageable pageable) {
        return userRepository.findAllByOrderByUserProfile_PointsDesc(pageable)
                .map(UserResponseDto::new);
    }

    // 주간 채택 수 TOP 3
    public List<UserResponseDto> getTop3LeaderboardByWeeklyAdopted() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<FeedbackRepository.UserFeedbackCount> topUsersStats = feedbackRepository.findTop3UsersByAdoptedFeedbackCount(oneWeekAgo);
        return getUsersInOrderFromStats(topUsersStats);
    }
    // 주간 등록 수 TOP 3
    public List<UserResponseDto> getTop3LeaderboardByWeeklyFeedback() {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        List<FeedbackRepository.UserFeedbackCount> topUsersStats = feedbackRepository.findTop3UsersByFeedbackCount(oneWeekAgo);
        return getUsersInOrderFromStats(topUsersStats);
    }

    // 주간 채택 수 페이징
    public Page<UserResponseDto> getLeaderboardByWeeklyAdopted(Pageable pageable) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        Page<FeedbackRepository.UserFeedbackCount> statsPage = feedbackRepository.findUsersByAdoptedFeedbackCount(oneWeekAgo, pageable);
        List<UserResponseDto> orderedUsers = getUsersInOrderFromStats(statsPage.getContent());
        return new PageImpl<>(orderedUsers, pageable, statsPage.getTotalElements());
    }
    // 주간 등록 수 페이징
    public Page<UserResponseDto> getLeaderboardByWeeklyFeedback(Pageable pageable) {
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);
        Page<FeedbackRepository.UserFeedbackCount> statsPage = feedbackRepository.findUsersByFeedbackCount(oneWeekAgo, pageable);
        List<UserResponseDto> orderedUsers = getUsersInOrderFromStats(statsPage.getContent());
        return new PageImpl<>(orderedUsers, pageable, statsPage.getTotalElements());
    }

    // FeedbackRepository 결과대로 User를 정렬하여 반환하도록 하는 헬퍼 메소드
    private List<UserResponseDto> getUsersInOrderFromStats(List<FeedbackRepository.UserFeedbackCount> userStats) {
        if (userStats.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> userIds = userStats.stream().map(FeedbackRepository.UserFeedbackCount::getUserId).toList();

        List<User> users = userRepository.findAllByIdWithProfile(userIds);

        return userIds.stream()
                .flatMap(id -> users.stream().filter(u -> u.getId().equals(id)))
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
    }

    // ===== 관리자 기능 =====
    @Transactional
    public void deleteUserByAdmin(Long userIdToDelete, String adminUsername) {
        User adminUser = userRepository.findByUserName(adminUsername)
                .orElseThrow(() -> new IllegalArgumentException("관리자 계정을 찾을 수 없습니다."));

        if (adminUser.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("사용자 삭제 권한이 없습니다.");
        }

        User userToDelete = userRepository.findById(userIdToDelete)
                .orElseThrow(() -> new IllegalArgumentException("삭제할 사용자를 찾을 수 없습니다. ID: " + userIdToDelete));

        if (userToDelete.getId().equals(adminUser.getId())) {
            throw new IllegalArgumentException("자기 자신을 삭제할 수 없습니다.");
        }

        userRepository.delete(userToDelete);

        // TODO: 사용자가 작성한 게시물, 피드백, 댓글, 좋아요 등을 어떻게 처리할지 정책 결정 필요
        //       (예: 같이 삭제, null로 변경, '탈퇴한 사용자'로 표시 등)
    }
}