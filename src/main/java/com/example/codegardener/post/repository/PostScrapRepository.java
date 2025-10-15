package com.example.codegardener.post.repository;

import com.example.codegardener.post.domain.PostScrap;
import com.example.codegardener.post.domain.PostScrapId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PostScrapRepository extends JpaRepository<PostScrap, PostScrapId> {
    long countByPostId(Long postId); // 사용 고민중...
    List<PostScrap> findAllByUserId(Long userId);
}