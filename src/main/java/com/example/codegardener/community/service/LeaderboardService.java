package com.example.codegardener.community.service;

import com.example.codegardener.user.dto.UserResponseDto;
import com.example.codegardener.user.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final UserService userService;

    public List<UserResponseDto> getTop3Leaderboard(String sortBy) {
        return switch (sortBy.toLowerCase()) {
            case "points" -> userService.getTop3LeaderboardByPoints();
            case "weeklyadopted" -> userService.getTop3LeaderboardByWeeklyAdopted();
            case "weeklyfeedback" -> userService.getTop3LeaderboardByWeeklyFeedback();
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다: " + sortBy);
        };
    }

    public Page<UserResponseDto> getLeaderboard(String sortBy, Pageable pageable) {
        return switch (sortBy.toLowerCase()) {
            case "points" -> userService.getLeaderboardByPoints(pageable);
            case "weeklyadopted" -> userService.getLeaderboardByWeeklyAdopted(pageable);
            case "weeklyfeedback" -> userService.getLeaderboardByWeeklyFeedback(pageable);
            default -> throw new IllegalArgumentException("지원하지 않는 정렬 기준입니다: " + sortBy);
        };
    }
}