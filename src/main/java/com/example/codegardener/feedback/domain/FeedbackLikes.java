package com.example.codegardener.feedback.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "feedback_likes",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "feedback_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackLikes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long feedbackLikeId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "feedback_id", nullable = false)
    private Feedback feedback;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
}
