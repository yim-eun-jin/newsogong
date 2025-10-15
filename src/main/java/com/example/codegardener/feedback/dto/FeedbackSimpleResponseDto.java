package com.example.codegardener.feedback.dto;

import com.example.codegardener.feedback.domain.Feedback;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FeedbackSimpleResponseDto {

    private final Long feedbackId;
    private final Long postId;
    private final String contentSummary;
    private final LocalDateTime createdAt;
    private final boolean isAdopted;

    public FeedbackSimpleResponseDto(Feedback feedback) {
        this.feedbackId = feedback.getUserId();
        this.postId = feedback.getPostId();
        this.createdAt = feedback.getCreatedAt();
        this.isAdopted = feedback.getAdoptedTF();

        // 내용이 100자 이상이면 잘라서 요약
        if (feedback.getContent() != null && feedback.getContent().length() > 100) {
            this.contentSummary = feedback.getContent().substring(0, 100) + "...";
        } else {
            this.contentSummary = feedback.getContent();
        }
    }
}