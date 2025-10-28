package com.example.codegardener.post.domain;

import java.time.LocalDateTime;
import jakarta.persistence.*;

import lombok.*;

import com.example.codegardener.user.domain.User;

@Entity
@Table (name = "post")
@Getter
@Setter
@NoArgsConstructor (access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Post {
    // 1. 기본 정보
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId; // 게시물 ID (PK)

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;  // 게시물 작성자 (FK)

    // 2. 필수 입력 정보
    @Column (nullable = false, length = 200)
    private String title;  // 제목

    @Column (nullable = false, columnDefinition = "TEXT")
    private String content;  // 내용

    @Column (nullable = false, columnDefinition = "TEXT")
    private String code; // 코드 스니펫
    
    @Column (nullable = false)
    private Boolean contentsType; // 게시물 카테고리 (true: 개발, false: 코딩테스트)

    @Column(length = 500)
    private String langTags;  // 프로그래밍 언어 태그

    @Column(length = 500)
    private String stackTags;  // 기술 스택 태그

    @Column (nullable = false, columnDefinition = "TEXT")
    private String summary;  // 피드백 받고 싶은 부분

    // 3. 선택 입력 정보
    @Column (length = 300)
    private String githubRepoUrl; // 깃허브 레포지토리 주소

    @Column (columnDefinition = "TEXT")
    private String problemStatement; // 코딩테스트 문제 원문 (contentsType=false 일 때 사용)

    @Column(columnDefinition = "LONGTEXT")
    private String aiFeedback; // AI 생성 피드백 (텍스트만 저장)

    // 4. 집계 데이터
    @Column (nullable = false)
    private int likesCount = 0;  // 좋아요 수

    @Column (nullable = false)
    private int views = 0;  // 조회수

    @Column (nullable = false)
    private int scrapCount = 0;  // 스크랩 수

    @Column (nullable = false)
    private int feedbackCount = 0;  // 달린 피드백 수

    // 5. 타임스탬프
    @Column (updatable = false, nullable = false)
    private LocalDateTime createdAt;  // 작성 시간

    @Column (nullable = false)
    private LocalDateTime modifiedAt;  // 최종 수정 시간

    @PrePersist
    protected void onCreate(){
        this.createdAt = LocalDateTime.now();
        this.modifiedAt = this.createdAt;
    }

    @PreUpdate
    public void onUpdate(){
        this.modifiedAt = LocalDateTime.now();
    }
}
