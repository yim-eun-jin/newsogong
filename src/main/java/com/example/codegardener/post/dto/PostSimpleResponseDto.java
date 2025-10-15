package com.example.codegardener.post.dto;

import com.example.codegardener.post.domain.Post;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostSimpleResponseDto {

    // Post 정보
    private final Long postId;
    private final String title;
    private final String summary; // 카드에 보일 요약 내용
    private final LocalDateTime createdAt;

    // 작성자 정보
    private final String username;
    private final String userProfileImage;

    // 통계 정보
    private final int likesCount;
    private final int scrapsCount;
    private final int feedbacksCount;
    private final int viewsCount;

    // 태그들 관련 수정 필요... 밑에도
    // private final List<String> tags;

    // Post 엔티티를 DTO로 변환
    public PostSimpleResponseDto(Post post) {
        this.postId = post.getId();
        this.title = post.getTitle();

        // 내용(content)이 150자(프론트랑 논의!) 이상이면 잘라서 요약(summary)
        if (post.getContent() != null && post.getContent().length() > 150) {
            this.summary = post.getContent().substring(0, 150) + "...";
        } else {
            this.summary = post.getContent();
        }

        this.createdAt = post.getCreatedAt();
        this.likesCount = post.getLikesCount();
        this.scrapsCount = post.getScrapCount();
        this.feedbacksCount = post.getFeedbacksCount();
        this.viewsCount = post.getViews();
        
        
        if (post.getUser() != null) {
            this.username = post.getUser().getUserName();
            // UserProfile이 null이 아닌지 확인
            if (post.getUser().getUserProfile() != null) {
                this.userProfileImage = post.getUser().getUserProfile().getUserPicture();
            } else {
                this.userProfileImage = null; // 기본 이미지 경로를 설정! 이것도 프론트랑 논의
            }
        } else {
            this.username = "알 수 없는 사용자";
            this.userProfileImage = null;
        }

        this.langtags = post.getLangTags();
        this.stackTags = post.getStackTags();
    }
}