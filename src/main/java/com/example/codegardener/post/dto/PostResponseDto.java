package com.example.codegardener.post.dto;

import com.example.codegardener.post.domain.Post;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class PostResponseDto {
    private Long postId;
    private Long userId;

    private String title;
    private String content;

    private String languages;
    private String stacks;

    private String code;
    private String summary;
    private Boolean contentsType;

    private String githubRepoUrl;
    private String problemStatement;

    private int likesCount;
    private int views;
    private int scrapCount;
    private int feedbackCount;

    private LocalDateTime createdAt;
    private LocalDateTime modifiedAt;

    private String aiFeedback;


    public static PostResponseDto from(Post post) {
        return new PostResponseDto(
                post.getPostId(),
                post.getUser().getId(),
                post.getTitle(),
                post.getContent(),
                post.getLangTags(),
                post.getStackTags(),
                post.getCode(),
                post.getSummary(),
                post.getContentsType(),
                post.getGithubRepoUrl(),
                post.getProblemStatement(),
                post.getLikesCount(),
                post.getViews(),
                post.getScrapCount(),
                post.getFeedbackCount(),
                post.getCreatedAt(),
                post.getModifiedAt(),
                post.getAiFeedback()
        );
    }
}