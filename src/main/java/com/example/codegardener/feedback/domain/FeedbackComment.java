package com.example.codegardener.feedback.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_comment")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long commentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback; // 어떤 피드백의 댓글인지

    @Column(nullable = false)
    private Long userId; // 작성자 ID

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
