package com.example.codegardener.user.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "UserProfile")
@Getter
@Setter
@NoArgsConstructor
public class UserProfile {

    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // User의 ID를 UserProfile의 ID로 사용
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "user_picture", length = 300)
    private String userPicture;

    private Integer points = 0;

    @Column(length = 20)
    private String grade;

    @Column(name = "post_count")
    private Integer postCount = 0;

    @Column(name = "totalfeedback_count")
    private Integer totalFeedbackCount = 0;

    @Column(name = "adopted_feedback_count")
    private Integer adoptedFeedbackCount = 0;
}