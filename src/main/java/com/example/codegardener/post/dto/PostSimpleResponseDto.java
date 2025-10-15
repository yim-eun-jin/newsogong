package com.example.codegardener.post.dto;

import com.example.codegardener.post.domain.Post;
import com.example.codegardener.user.domain.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class PostSimpleResponseDto {

    // Post 정보
    private final Long postId;
    private final String title;
    private final String summary;
    private final LocalDateTime createdAt;

    // 작성자 정보
    private final String username;
    private final String userProfileImage;

    // 통계
    private final int likesCount;
    private final int scrapsCount;
    private final int feedbacksCount;
    private final int viewsCount;

    // 태그
    private final String langTags;
    private final String stackTags;

    public PostSimpleResponseDto(Post post) {
        this.postId = post.getPostId();
        this.title = post.getTitle();

        String content = post.getContent();
        this.summary = (content != null && content.length() > 150)
                ? content.substring(0, 150) + "..."
                : content;

        this.createdAt = post.getCreatedAt();
        this.likesCount = post.getLikesCount();
        this.scrapsCount = post.getScrapCount();
        this.feedbacksCount = post.getFeedbackCount();
        this.viewsCount = post.getViews();

        // ✅ 연관 엔티티(User)로 접근
        User user = post.getUser();
        if (user != null) {
            this.username = user.getUserName();
            this.userProfileImage = (user.getUserProfile() != null)
                    ? user.getUserProfile().getUserPicture()
                    : null;
        } else {
            this.username = "알 수 없는 사용자";
            this.userProfileImage = null;
        }

        this.langTags = post.getLangTags();
        this.stackTags = post.getStackTags();
    }
}