package com.example.codegardener.feedback.controller;

import com.example.codegardener.feedback.dto.*;
import com.example.codegardener.feedback.service.FeedbackService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/feedback")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:3000") // ✅ React 연동을 위해 추가
public class FeedbackController {

    private final FeedbackService feedbackService;

    /**
     * ✅ [POST] 피드백 작성
     */
    @PostMapping
    public ResponseEntity<FeedbackResponseDto> createFeedback(@RequestBody FeedbackRequestDto dto) {
        return ResponseEntity.ok(feedbackService.createFeedback(dto));
    }

    /**
     * ✅ [PUT] 피드백 수정
     */
    @PutMapping("/{feedbackId}")
    public ResponseEntity<FeedbackResponseDto> updateFeedback(
            @PathVariable Long feedbackId,
            @RequestBody FeedbackRequestDto dto
    ) {
        return ResponseEntity.ok(feedbackService.updateFeedback(feedbackId, dto));
    }

    /**
     * ✅ [DELETE] 피드백 삭제
     */
    @DeleteMapping("/{feedbackId}")
    public ResponseEntity<String> deleteFeedback(
            @PathVariable Long feedbackId,
            @RequestParam Long userId
    ) {
        feedbackService.deleteFeedback(feedbackId, userId);
        return ResponseEntity.ok("피드백이 삭제되었습니다.");
    }

    /**
     * ✅ [GET] 피드백 상세조회 (라인피드백 + 댓글 포함)
     */
    @GetMapping("/{feedbackId}")
    public ResponseEntity<FeedbackDetailResponseDto> getFeedbackDetail(@PathVariable Long feedbackId) {
        return ResponseEntity.ok(feedbackService.getFeedbackDetail(feedbackId));
    }

    /**
     * ✅ [GET] 게시물별 피드백 목록 조회
     */
    @GetMapping("/post/{postId}")
    public ResponseEntity<List<FeedbackResponseDto>> getFeedbackListByPost(@PathVariable Long postId) {
        return ResponseEntity.ok(feedbackService.getFeedbackListByPost(postId));
    }

    /**
     * ✅ [POST] 좋아요 토글
     */
    @PostMapping("/{feedbackId}/like")
    public ResponseEntity<String> toggleLike(
            @PathVariable Long feedbackId,
            @RequestParam Long userId
    ) {
        return ResponseEntity.ok(feedbackService.toggleLike(feedbackId, userId));
    }

    // ======================================================
    // 🧩 라인피드백(LineFeedback) 관련 API
    // ======================================================

    /**
     * ✅ [POST] 라인피드백 등록
     * URL: /api/feedback/{feedbackId}/line
     */
    @PostMapping("/{feedbackId}/line")
    public ResponseEntity<LineFeedbackDto> addLineFeedback(
            @PathVariable Long feedbackId,
            @RequestBody LineFeedbackDto dto
    ) {
        return ResponseEntity.ok(feedbackService.addLineFeedback(feedbackId, dto));
    }

    /**
     * ✅ [GET] 특정 피드백에 속한 라인피드백 목록 조회
     * URL: /api/feedback/{feedbackId}/lines
     */
    @GetMapping("/{feedbackId}/lines")
    public ResponseEntity<List<LineFeedbackDto>> getLineFeedbacks(
            @PathVariable Long feedbackId
    ) {
        return ResponseEntity.ok(feedbackService.getLineFeedbacks(feedbackId));
    }

    /**
     * ✅ [PUT] 라인피드백 수정
     * URL: /api/feedback/{feedbackId}/line/{lineFeedbackId}
     */
    @PutMapping("/{feedbackId}/line/{lineFeedbackId}")
    public ResponseEntity<LineFeedbackDto> updateLineFeedback(
            @PathVariable Long feedbackId,
            @PathVariable Long lineFeedbackId,
            @RequestBody LineFeedbackDto dto
    ) {
        return ResponseEntity.ok(feedbackService.updateLineFeedback(feedbackId, lineFeedbackId, dto));
    }

    /**
     * ✅ [DELETE] 라인피드백 삭제
     * URL: /api/feedback/{feedbackId}/line/{lineFeedbackId}
     */
    @DeleteMapping("/{feedbackId}/line/{lineFeedbackId}")
    public ResponseEntity<String> deleteLineFeedback(
            @PathVariable Long feedbackId,
            @PathVariable Long lineFeedbackId,
            @RequestParam Long userId
    ) {
        feedbackService.deleteLineFeedback(feedbackId, lineFeedbackId, userId);
        return ResponseEntity.ok("라인피드백이 삭제되었습니다.");
    }

    // ======================================================
    // 💬 댓글(FeedbackComment) 관련 API
    // ======================================================

    /**
     * ✅ [POST] 댓글 추가
     */
    @PostMapping("/{feedbackId}/comment")
    public ResponseEntity<FeedbackCommentDto> addComment(
            @PathVariable Long feedbackId,
            @RequestBody FeedbackCommentDto dto
    ) {
        return ResponseEntity.ok(feedbackService.addComment(feedbackId, dto));
    }

    /**
     * ✅ [GET] 댓글 목록 조회
     */
    @GetMapping("/{feedbackId}/comments")
    public ResponseEntity<List<FeedbackCommentDto>> getCommentsByFeedback(
            @PathVariable Long feedbackId
    ) {
        return ResponseEntity.ok(feedbackService.getCommentsByFeedback(feedbackId));
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<List<FeedbackSimpleResponseDto>> getFeedbacksByUser(@PathVariable Long userId) {
        return ResponseEntity.ok(feedbackService.getFeedbacksByUserId(userId));
    }
}
