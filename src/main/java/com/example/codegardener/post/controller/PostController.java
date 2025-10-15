package com.example.codegardener.post.controller;

import com.example.codegardener.post.dto.PostRequestDto;
import com.example.codegardener.post.dto.PostResponseDto;
import com.example.codegardener.post.service.PostService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

/**
 * 게시물 관련 REST 컨트롤러
 * - CRUD
 * - 통합 검색 (/posts/search)
 * - AI 피드백 관리 (/posts/{id}/ai)
 */
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    /** 정렬 키 */
    public enum SortKey { latest, views, feedback }

    // ====================== CREATE ======================
    @PostMapping
    public ResponseEntity<PostResponseDto> create(@Valid @RequestBody PostRequestDto dto) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("userId는 필수입니다.");
        }
        PostResponseDto created = postService.create(dto);
        return ResponseEntity
                .created(URI.create("/posts/" + created.getPostId()))
                .body(created);
    }

    // ====================== READ ======================

    /** 전체 목록 (비페이징) */
    @GetMapping
    public List<PostResponseDto> list() {
        return postService.list();
    }

    /** 페이징 목록 — contentsType: null=전체 / true=개발 / false=코테 */
    @GetMapping("/page")
    public Page<PostResponseDto> listPaged(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) Boolean contentsType,
            @RequestParam(defaultValue = "latest") String sort
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        SortKey sortKey;
        try {
            sortKey = SortKey.valueOf(sort.toLowerCase());
        } catch (Exception e) {
            sortKey = SortKey.latest;
        }

        return postService.listPaged(safePage, safeSize, contentsType, sortKey.name());
    }

    /** 게시물 상세 */
    @GetMapping("/{id}")
    public PostResponseDto get(@PathVariable Long id) {
        return postService.get(id);
    }

    // ====================== UPDATE ======================
    @PutMapping("/{id}")
    public PostResponseDto update(
            @PathVariable Long id,
            @Valid @RequestBody PostRequestDto dto
    ) {
        if (dto.getUserId() == null) {
            throw new IllegalArgumentException("수정 권한 확인을 위해 userId가 필요합니다.");
        }
        return postService.update(id, dto, dto.getUserId());
    }

    // ====================== DELETE ======================
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam Long userId
    ) {
        if (userId == null) {
            throw new IllegalArgumentException("삭제 권한 확인을 위해 userId가 필요합니다.");
        }
        postService.delete(id, userId);
        return ResponseEntity.noContent().build(); // 204
    }

    // ====================== SEARCH (통합 검색) ======================
    /**
     * 통합 검색 엔드포인트
     * - 키워드, 언어, 스택, 탭, 정렬, 페이징 모두 지원
     * - 예시:
     *   /posts/search?q=spring
     *   /posts/search?languages=java&languages=python
     *   /posts/search?langs=java,python&tech=docker,springboot&contentsType=true&order=popular&page=0&size=12
     */
    @GetMapping("/search")
    public Page<PostResponseDto> searchUnified(
            @RequestParam(required = false) String q,                              // 키워드
            @RequestParam(name = "languages", required = false) List<String> languages, // 배열형 언어
            @RequestParam(name = "langs", required = false) String langsCsv,           // CSV형 언어
            @RequestParam(name = "stacks", required = false) List<String> stacks,      // 배열형 스택
            @RequestParam(name = "tech", required = false) String stacksCsv,           // CSV형 스택
            @RequestParam(required = false) Boolean contentsType,                     // true=개발, false=코테
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(name = "order", defaultValue = "recent") String order
    ) {
        // order → 내부 정렬 키 매핑
        String sort = switch (order.toLowerCase()) {
            case "popular"  -> "views";
            case "feedback" -> "feedback";
            default         -> "latest";
        };

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        return postService.discoverAdvanced(
                q, languages, langsCsv, stacks, stacksCsv, contentsType,
                safePage, safeSize, sort
        );
    }

    // AI 피드백

    // AI 피드백 재생성
    @PostMapping("/{id}/ai")
    public PostResponseDto regenerateAi(
            @PathVariable Long id,
            @RequestParam(required = false) Long requesterId
    ) {
        return postService.generateAiFeedback(id, requesterId);
    }

    //저장된 AI 피드백 조회
    @GetMapping("/{id}/ai")
    public String getAiFeedback(@PathVariable Long id) {
        String feedback = postService.getAiFeedback(id);
        return (feedback != null)
                ? feedback
                : "AI 피드백이 아직 생성되지 않았습니다.";
    }
}