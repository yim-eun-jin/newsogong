package com.example.codegardener.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PostRequestDto {

    @NotNull(message = "userId는 필수입니다.")
    private Long userId;

    @NotBlank(message = "제목은 필수입니다.")
    private String title;

    @NotBlank(message = "설명(본문)은 필수입니다.")
    private String content; // 게시물 설명

    @NotBlank(message = "프로그래밍 언어는 필수입니다.")
    private String languages;   // 프로그래밍언어

    private String stacks;  // 기술스택

    @NotBlank(message = "코드는 필수입니다.")
    private String code;

    @NotBlank(message = "피드백 요청 요약은 필수입니다.")
    private String summary;

    @NotNull(message = "게시물 종류는 필수입니다.")
    private Boolean contentsType; // true=개발, false=코테

    // 선택 항목
    private String githubRepoUrl;
    private String problemStatement;
}