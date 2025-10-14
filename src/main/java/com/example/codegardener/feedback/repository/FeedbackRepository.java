package com.example.codegardener.feedback.repository;

import com.example.codegardener.feedback.domain.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // 특정 게시물에 달린 피드백 전체 조회
    List<Feedback> findByPostId(Long postId);

    // 특정 사용자가 작성한 피드백 전체 조회
    List<Feedback> findByUserId(Long userId);
}
