package com.example.codegardener.feedback.repository;

import com.example.codegardener.feedback.domain.FeedbackComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedbackCommentRepository extends JpaRepository<FeedbackComment, Long> {

    // 특정 피드백에 달린 모든 댓글 조회
    List<FeedbackComment> findByFeedback_FeedbackId(Long feedbackId);
}
