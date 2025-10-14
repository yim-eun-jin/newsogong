package com.example.codegardener.feedback.service;

import com.example.codegardener.feedback.domain.*;
import com.example.codegardener.feedback.dto.*;
import com.example.codegardener.feedback.repository.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final FeedbackLikesRepository feedbackLikesRepository;
    private final LineFeedbackRepository lineFeedbackRepository;
    private final FeedbackCommentRepository feedbackCommentRepository;

    // ============================================================
    // ✅ 피드백 CRUD
    // ============================================================

    // ✅ 피드백 작성
    public FeedbackResponseDto createFeedback(FeedbackRequestDto dto) {
        Feedback feedback = dto.toEntity();
        return FeedbackResponseDto.fromEntity(feedbackRepository.save(feedback));
    }

    // ✅ 피드백 수정 (채택된 피드백은 수정 불가)
    public FeedbackResponseDto updateFeedback(Long feedbackId, FeedbackRequestDto dto) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다."));

        if (!feedback.getUserId().equals(dto.getUserId())) {
            throw new IllegalStateException("본인만 피드백을 수정할 수 있습니다.");
        }
        if (feedback.getAdoptedTF()) {
            throw new IllegalStateException("채택된 피드백은 수정할 수 없습니다.");
        }

        feedback.setContent(dto.getContent());
        feedback.setRating(dto.getRating());
        feedback.setUpdatedAt(java.time.LocalDateTime.now());

        return FeedbackResponseDto.fromEntity(feedbackRepository.save(feedback));
    }

    // ✅ 피드백 삭제
    public void deleteFeedback(Long feedbackId, Long userId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다."));
        if (!feedback.getUserId().equals(userId)) {
            throw new IllegalStateException("본인만 피드백을 삭제할 수 있습니다.");
        }
        if (feedback.getAdoptedTF()) {
            throw new IllegalStateException("채택된 피드백은 삭제할 수 없습니다.");
        }
        feedbackRepository.delete(feedback);
    }

    // ✅ 피드백 상세조회 (라인피드백 + 댓글 포함)
    public FeedbackDetailResponseDto getFeedbackDetail(Long feedbackId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다."));
        return FeedbackDetailResponseDto.fromEntity(feedback);
    }

    // ✅ 게시물별 피드백 목록 조회
    public List<FeedbackResponseDto> getFeedbackListByPost(Long postId) {
        return feedbackRepository.findByPostId(postId).stream()
                .map(FeedbackResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ✅ 좋아요 토글
    public String toggleLike(Long feedbackId, Long userId) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다."));

        Optional<FeedbackLikes> existingLike =
                feedbackLikesRepository.findByUserIdAndFeedback_FeedbackId(userId, feedbackId);

        if (existingLike.isPresent()) {
            feedbackLikesRepository.delete(existingLike.get());
            feedback.setLikesCount(feedback.getLikesCount() - 1);
            feedbackRepository.save(feedback);
            return "좋아요 취소";
        } else {
            FeedbackLikes like = FeedbackLikes.builder()
                    .feedback(feedback)
                    .userId(userId)
                    .build();
            feedbackLikesRepository.save(like);
            feedback.setLikesCount(feedback.getLikesCount() + 1);
            feedbackRepository.save(feedback);
            return "좋아요 추가";
        }
    }

    // ============================================================
    // 🧩 라인피드백 CRUD
    // ============================================================

    // ✅ [CREATE] 라인피드백 등록
    public LineFeedbackDto addLineFeedback(Long feedbackId, LineFeedbackDto dto) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다."));

        LineFeedback lineFeedback = LineFeedback.builder()
                .feedback(feedback)
                .userId(dto.getUserId())
                .lineNumber(dto.getLineNumber())
                .endLineNumber(dto.getEndLineNumber())
                .content(dto.getContent())
                .build();

        return LineFeedbackDto.fromEntity(lineFeedbackRepository.save(lineFeedback));
    }

    // ✅ [READ] 특정 피드백의 라인피드백 목록 조회
    public List<LineFeedbackDto> getLineFeedbacks(Long feedbackId) {
        return lineFeedbackRepository.findByFeedback_FeedbackId(feedbackId)
                .stream()
                .map(LineFeedbackDto::fromEntity)
                .collect(Collectors.toList());
    }

    // ✅ [UPDATE] 라인피드백 수정
    public LineFeedbackDto updateLineFeedback(Long feedbackId, Long lineFeedbackId, LineFeedbackDto dto) {
        LineFeedback lineFeedback = lineFeedbackRepository.findById(lineFeedbackId)
                .orElseThrow(() -> new IllegalArgumentException("라인피드백을 찾을 수 없습니다."));

        if (!lineFeedback.getFeedback().getFeedbackId().equals(feedbackId)) {
            throw new IllegalStateException("잘못된 피드백 ID입니다.");
        }
        if (!lineFeedback.getUserId().equals(dto.getUserId())) {
            throw new IllegalStateException("본인만 수정할 수 있습니다.");
        }

        lineFeedback.setContent(dto.getContent());
        lineFeedback.setEndLineNumber(dto.getEndLineNumber());
        return LineFeedbackDto.fromEntity(lineFeedbackRepository.save(lineFeedback));
    }

    // ✅ [DELETE] 라인피드백 삭제
    public void deleteLineFeedback(Long feedbackId, Long lineFeedbackId, Long userId) {
        LineFeedback lineFeedback = lineFeedbackRepository.findById(lineFeedbackId)
                .orElseThrow(() -> new IllegalArgumentException("라인피드백을 찾을 수 없습니다."));

        if (!lineFeedback.getFeedback().getFeedbackId().equals(feedbackId)) {
            throw new IllegalStateException("잘못된 피드백 ID입니다.");
        }
        if (!lineFeedback.getUserId().equals(userId)) {
            throw new IllegalStateException("본인만 삭제할 수 있습니다.");
        }

        lineFeedbackRepository.delete(lineFeedback);
    }

    // ============================================================
    // 💬 댓글(Comment) CRUD
    // ============================================================

    // ✅ 댓글 작성
    public FeedbackCommentDto addComment(Long feedbackId, FeedbackCommentDto dto) {
        Feedback feedback = feedbackRepository.findById(feedbackId)
                .orElseThrow(() -> new IllegalArgumentException("피드백을 찾을 수 없습니다."));

        FeedbackComment comment = FeedbackComment.builder()
                .feedback(feedback)
                .userId(dto.getUserId())
                .content(dto.getContent())
                .build();

        return FeedbackCommentDto.fromEntity(feedbackCommentRepository.save(comment));
    }

    // ✅ 댓글 목록 조회
    public List<FeedbackCommentDto> getCommentsByFeedback(Long feedbackId) {
        return feedbackCommentRepository.findByFeedback_FeedbackId(feedbackId).stream()
                .map(FeedbackCommentDto::fromEntity)
                .collect(Collectors.toList());
    }
}
