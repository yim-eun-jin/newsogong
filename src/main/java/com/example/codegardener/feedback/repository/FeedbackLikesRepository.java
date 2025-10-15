package com.example.codegardener.feedback.repository;

import com.example.codegardener.feedback.domain.FeedbackLikes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackLikesRepository extends JpaRepository<FeedbackLikes, Long> {

    // 특정 유저가 해당 피드백에 이미 좋아요를 눌렀는지 확인
    Optional<FeedbackLikes> findByUserIdAndFeedback_FeedbackId(Long userId, Long feedbackId);

    // 특정 피드백에 눌린 좋아요 전체 개수 세기
    Long countByFeedback_FeedbackId(Long feedbackId);

    // 특정 유저의 모든 좋아요 목록
    List<FeedbackLikes> findByUserId(Long userId);
}
