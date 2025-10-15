package com.example.codegardener.feedback.repository;

import com.example.codegardener.feedback.domain.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    // 특정 게시물에 달린 피드백 전체 조회
    List<Feedback> findByPostId(Long postId);

    // 특정 사용자가 작성한 피드백 전체 조회
    List<Feedback> findByUserId(Long userId);

    // JPQL 집계 결과를 담기 위한 인터페이스
    interface UserFeedbackCount {
        Long getUserId();
        Long getCount();
    }

    // 주간 등록 수 TOP3
    @Query("SELECT f.userId AS userId, COUNT(f) AS count FROM Feedback f WHERE f.createdAt >= :startDate GROUP BY f.userId ORDER BY count DESC LIMIT 3")
    List<UserFeedbackCount> findTop3UsersByFeedbackCount(@Param("startDate") LocalDateTime startDate);
    // 주간 등록 수 페이징
    @Query(value = "SELECT f.userId AS userId, COUNT(f) AS count FROM Feedback f WHERE f.createdAt >= :startDate GROUP BY f.userId ORDER BY count DESC",
            countQuery = "SELECT COUNT(DISTINCT f.userId) FROM Feedback f WHERE f.createdAt >= :startDate")
    Page<UserFeedbackCount> findUsersByFeedbackCount(@Param("startDate") LocalDateTime startDate, Pageable pageable);

    // 주간 채택 수 TOP3
    @Query("SELECT f.userId AS userId, COUNT(f) AS count FROM Feedback f WHERE f.adoptedTF = true AND f.createdAt >= :startDate GROUP BY f.userId ORDER BY count DESC LIMIT 3")
    List<UserFeedbackCount> findTop3UsersByAdoptedFeedbackCount(@Param("startDate") LocalDateTime startDate);
    // 주간 채택 수 페이징
    @Query(value = "SELECT f.userId AS userId, COUNT(f) AS count FROM Feedback f WHERE f.adoptedTF = true AND f.createdAt >= :startDate GROUP BY f.userId ORDER BY count DESC",
            countQuery = "SELECT COUNT(DISTINCT f.userId) FROM Feedback f WHERE f.adoptedTF = true AND f.createdAt >= :startDate")
    Page<UserFeedbackCount> findUsersByAdoptedFeedbackCount(@Param("startDate") LocalDateTime startDate, Pageable pageable);
}
