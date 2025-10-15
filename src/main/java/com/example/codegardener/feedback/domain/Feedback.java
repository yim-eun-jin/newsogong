package com.example.codegardener.feedback.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "feedback")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Feedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackId;

    @Column(nullable = false)
    private Long postId; // 어떤 게시물에 대한 피드백인지

    @Column(nullable = false)
    private Long userId; // 피드백 작성자 ID

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content; // 전체 피드백 내용

    @Column(nullable = false)
    private Double rating; // 평점 (0.5 ~ 5.0)

    @Column(nullable = false)
    private Boolean adoptedTF = false; // 채택 여부

    @Column(nullable = false)
    private Integer likesCount = 0; // 좋아요 수

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    // ===== 연관관계 ===== //
    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LineFeedback> lineFeedbackList = new ArrayList<>();

    @OneToMany(mappedBy = "feedback", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FeedbackComment> comments = new ArrayList<>();

    // ===== 생명주기 콜백 ===== //
    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
