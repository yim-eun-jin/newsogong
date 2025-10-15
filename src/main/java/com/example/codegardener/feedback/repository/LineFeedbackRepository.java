package com.example.codegardener.feedback.repository;

import com.example.codegardener.feedback.domain.LineFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LineFeedbackRepository extends JpaRepository<LineFeedback, Long> {

    // 특정 Feedback에 속한 모든 라인 피드백 조회
    List<LineFeedback> findByFeedback_FeedbackId(Long feedbackId);
}
