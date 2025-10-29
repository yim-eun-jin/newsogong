package com.example.codegardener.user.service;

import java.time.LocalDate;
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
import lombok.extern.slf4j.Slf4j;

import com.example.codegardener.feedback.repository.FeedbackRepository;
import com.example.codegardener.global.jwt.JwtUtil;
import com.example.codegardener.user.domain.Role;
import com.example.codegardener.user.domain.User;
import com.example.codegardener.user.domain.UserProfile;
import com.example.codegardener.user.dto.LoginRequestDto;
import com.example.codegardener.user.dto.SignUpRequestDto;
import com.example.codegardener.user.dto.UserResponseDto;
import com.example.codegardener.user.repository.UserRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final FeedbackRepository feedbackRepository;

    private static final String GRADE_SEED = "새싹 개발자";
    private static final String GRADE_LEAF = "잎새 개발자";
    private static final String GRADE_TREE = "나무 개발자";
    private static final String GRADE_SAGE = "숲의 현자";

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

        UserProfile userProfile = new UserProfile();
        userProfile.setUser(newUser);
        userProfile.setPoints(1000);
        newUser.setUserProfile(userProfile);

        updateGrade(userProfile);

        User savedUser = userRepository.save(newUser);
        log.info("New user signed up: {} (ID: {}), initial points: 1000, grade: {}",
                savedUser.getUserName(), savedUser.getId(), savedUser.getUserProfile().getGrade());
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

    // 회원 탈퇴
    @Transactional
    public void deleteCurrentUser(String currentUsername) {
        User currentUser = userRepository.findByUserName(currentUsername)
                .orElseThrow(() -> new IllegalArgumentException("인증된 사용자를 찾을 수 없습니다."));

        if (currentUser.getRole() == Role.ADMIN) {
            throw new IllegalStateException("관리자 계정은 스스로 탈퇴할 수 없습니다. 다른 관리자에게 요청하세요.");
        }

        log.info("User '{}' (userId={}) is deleting their own account.",
                currentUser.getUserName(), currentUser.getId());
        userRepository.delete(currentUser);

        // TODO: 관리자 삭제와 동일하게, 사용자가 작성한 게시물, 피드백 등의 처리 정책 필요
    }

    // ===== 포인트 및 등급 관련 =====
    @Transactional
    public void addPoints(User user, int pointsToAdd, String reason) {
        if (pointsToAdd <= 0) {
            log.warn("Attempted to add non-positive points ({}) to user {}", pointsToAdd, user.getUserName());
            return;
        }

        UserProfile userProfile = getUserProfileEntity(user);
        int currentPoints = userProfile.getPoints();
        userProfile.setPoints(currentPoints + pointsToAdd);

        updateGrade(userProfile); // 등급 업데이트

        log.info("Added {} points to user {} for [{}]. New points: {}, New grade: {}",
                pointsToAdd, user.getUserName(), reason, userProfile.getPoints(), userProfile.getGrade());
    }

    @Transactional
    public void subtractPoints(User user, int pointsToSubtract, String reason) {
        if (pointsToSubtract <= 0) {
            log.warn("Attempted to subtract non-positive points ({}) from user {}", pointsToSubtract, user.getUserName());
            return;
        }

        UserProfile userProfile = getUserProfileEntity(user);
        int currentPoints = userProfile.getPoints();

        if (currentPoints < pointsToSubtract) {
            log.warn("User {} has insufficient points ({}) to subtract {}.", user.getUserName(), currentPoints, pointsToSubtract);
            userProfile.setPoints(0); // 0으로 만들기 (또는 예외 처리)
        } else {
            userProfile.setPoints(currentPoints - pointsToSubtract);
        }

        updateGrade(userProfile); // 등급 업데이트

        log.info("Subtracted {} points from user {} for [{}]. New points: {}, New grade: {}",
                pointsToSubtract, user.getUserName(), reason, userProfile.getPoints(), userProfile.getGrade());
    }

    private void updateGrade(UserProfile userProfile) {
        int points = userProfile.getPoints();
        String newGrade;

        if (points >= 10000) {
            newGrade = GRADE_SAGE;
        } else if (points >= 5000) {
            newGrade = GRADE_TREE;
        } else if (points >= 2000) {
            newGrade = GRADE_LEAF;
        } else {
            newGrade = GRADE_SEED;
        }

        if (!newGrade.equals(userProfile.getGrade())) {
            log.info("User {}'s grade changed from {} to {}",
                    userProfile.getUser().getUserName(), userProfile.getGrade(), newGrade);
            userProfile.setGrade(newGrade);
        }
    }

    private UserProfile getUserProfileEntity(User user) {
        UserProfile profile = user.getUserProfile();
        if (profile == null) {
            log.error("UserProfile not found for user: {}", user.getUserName());
            throw new IllegalStateException("사용자 프로필 정보를 찾을 수 없습니다.");
        }
        return profile;
    }

    @Transactional
    public void awardPointsForAdoptedFeedback(User feedbackAuthor) {
        addPoints(feedbackAuthor, 100, "피드백 채택");
        UserProfile profile = getUserProfileEntity(feedbackAuthor);
        profile.setAdoptedFeedbackCount(profile.getAdoptedFeedbackCount() + 1);
    }

    @Transactional
    public String recordAttendance(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        LocalDate today = LocalDate.now();
        UserProfile userProfile = getUserProfileEntity(user);

        // 마지막 출석일 확인
        if (userProfile.getLastAttendanceDate() != null && userProfile.getLastAttendanceDate().isEqual(today)) {
            return "오늘은 이미 출석했습니다.";
        }

        // 출석 처리: 포인트 추가 및 마지막 출석 날짜 업데이트
        addPoints(user, 50, "일일 출석");
        userProfile.setLastAttendanceDate(today);

        return "출석 완료! 50 포인트가 지급되었습니다.";
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

        log.warn("[ADMIN] Admin '{}' is deleting user '{}' (userId={})",
                adminUsername, userToDelete.getUserName(), userIdToDelete);

        userRepository.delete(userToDelete);

        // TODO: 사용자가 작성한 게시물, 피드백, 댓글, 좋아요 등을 어떻게 처리할지 정책 결정 필요
        //       (예: 같이 삭제, null로 변경, '탈퇴한 사용자'로 표시 등)
    }
}