package com.example.codegardener.community.repository;

import com.example.codegardener.community.entity.PostScrap;
import com.example.codegardener.community.entity.PostScrapId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostScrapRepository extends JpaRepository<PostScrap, PostScrapId> {

    // 특정 게시물의 스크랩 개수를 세기 위한 메서드
    long countByPostId(Long postId);
}