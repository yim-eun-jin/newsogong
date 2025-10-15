package com.example.codegardener.post.domain;

import com.example.codegardener.user.domain.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table (name = "post")
@Getter @Setter
@NoArgsConstructor (access = AccessLevel.PROTECTED)
@AllArgsConstructor @Builder
public class Post {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long postId; //PK

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false) // post.user_id FK
    private User user;

    //-----필수입력-----
    @Column (nullable = false, length = 200)
    private String title; //제목

    @Column (nullable = false, columnDefinition = "TEXT")
    private String content; //본문/설명

    @Column (nullable = false, columnDefinition = "TEXT")
    private String code; //코드 스니펫

    // 게시물 카테고리
    @Column (nullable = false)
    private Boolean contentsType; //true=개발용,  false=코테용

    @Column(length = 500)
    private String langTags;   // 프로그래밍 언어

    @Column(length = 500)
    private String stackTags;  // 기술 스택

    @Column (nullable = false, columnDefinition = "TEXT")
    private String summary; //피드백 원하는 내용

    //-----선택입력-----
    @Column (length = 300)
    private String githubRepoUrl; //깃허브 레포주소

    @Column (columnDefinition = "TEXT")
    private String problemStatement; //코딩테스트 문제 원문 (선택, 코테일때만사용)

    @Column(columnDefinition = "LONGTEXT")
    private String aiFeedback; // AI 생성 피드백 (텍스트만 저장)

    // 집계/표시용
    @Column (nullable = false)
    private int likesCount = 0;

    @Column (nullable = false)
    private int views = 0;

    @Column (nullable = false)
    private int scrapCount = 0;

    @Column (nullable = false)
    private int feedbackCount = 0;

    // 타임스탬프
    @Column (updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column (nullable = false)
    private LocalDateTime modifiedAt;

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
