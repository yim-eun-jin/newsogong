package com.example.codegardener.community.repository;

import com.example.codegardener.community.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    // 특정 사용자가 특정 게시물에 좋아요를 눌렀는지 확인하기 위한 메서드
    Optional<PostLike> findByUserIdAndPostId(Long userId, Long postId);

    // 특정 게시물의 좋아요 개수를 세기 위한 메서드
    long countByPostId(Long postId);
}