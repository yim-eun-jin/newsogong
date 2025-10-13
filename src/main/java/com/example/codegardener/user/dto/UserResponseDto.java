package com.example.codegardener.user.dto;

import com.example.codegardener.user.entity.User;
import com.example.codegardener.user.role.Role;
import lombok.Getter;

@Getter
public class UserResponseDto {

    private final Long id;
    private final String userName;
    private final String email;
    private final Role role;

    // UserProfile
    private final String userPicture;
    private final Integer points;
    private final String grade;
    private final Integer postCount;
    private final Integer totalFeedbackCount;
    private final Integer adoptedFeedbackCount;

    // User Entity를 UserResponseDto로 변환
    public UserResponseDto(User user) {
        this.id = user.getId();
        this.userName = user.getUserName();
        this.email = user.getEmail();
        this.role = user.getRole();

        if (user.getUserProfile() != null) {
            this.userPicture = user.getUserProfile().getUserPicture();
            this.points = user.getUserProfile().getPoints();
            this.grade = user.getUserProfile().getGrade();
            this.postCount = user.getUserProfile().getPostCount();
            this.totalFeedbackCount = user.getUserProfile().getTotalFeedbackCount();
            this.adoptedFeedbackCount = user.getUserProfile().getAdoptedFeedbackCount();
        } else {
            this.userPicture = null;
            this.points = 0;
            this.grade = null;
            this.postCount = 0;
            this.totalFeedbackCount = 0;
            this.adoptedFeedbackCount = 0;
        }
    }
}