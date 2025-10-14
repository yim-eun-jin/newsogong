package com.example.codegardener.feedback.dto;

import com.example.codegardener.feedback.domain.Feedback;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedbackRequestDto {

    private Long postId;
    private Long userId;
    private String content;
    private Double rating;

    // ✅ 변환 로직은 유지하되, Service에서만 호출할 것
    public Feedback toEntity() {
        return Feedback.builder()
                .postId(this.postId)
                .userId(this.userId)
                .content(this.content)
                .rating(this.rating)
                .adoptedTF(false)
                .likesCount(0)
                .build();
    }
}
