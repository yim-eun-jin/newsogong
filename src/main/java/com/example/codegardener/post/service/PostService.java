package com.example.codegardener.post.service;

import com.example.codegardener.ai.service.AiFeedbackService;
import com.example.codegardener.post.domain.Post;
import com.example.codegardener.post.domain.PostLike;
import com.example.codegardener.post.domain.PostScrap;
import com.example.codegardener.post.domain.PostScrapId;
import com.example.codegardener.post.dto.PostActionDto;
import com.example.codegardener.post.dto.PostRequestDto;
import com.example.codegardener.post.dto.PostResponseDto;
import com.example.codegardener.post.dto.PostSimpleResponseDto;
import com.example.codegardener.post.repository.PostLikeRepository;
import com.example.codegardener.post.repository.PostRepository;
import com.example.codegardener.post.repository.PostScrapRepository;
import com.example.codegardener.user.domain.User;
import com.example.codegardener.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;
    private final UserRepository userRepository;
    private final AiFeedbackService aiFeedbackService;

    // ===================== CRUD =====================

    @Transactional
    public PostResponseDto create(PostRequestDto dto) {
        validateCodingTest(dto);

        Post p = Post.builder()
                .userId(dto.getUserId())
                .title(dto.getTitle())
                .content(dto.getContent())
                .code(dto.getCode())
                .summary(dto.getSummary())
                .contentsType(dto.getContentsType())
                .githubRepoUrl(dto.getGithubRepoUrl())
                .problemStatement(dto.getProblemStatement())
                .langTags(normalizeCsv(dto.getLanguages()))
                .stackTags(normalizeCsv(dto.getStacks()))
                .build();

        Post saved = postRepository.save(p);
        log.info("[POST] saved postId={}", saved.getPostId());
        return PostResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<PostResponseDto> list() {
        return postRepository.findAll()
                .stream()
                .map(PostResponseDto::from)
                .toList();
    }

    /**
     * 페이징 목록
     * contentsType: null=전체, true=개발, false=코테
     * sortBy: latest | views | feedback
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDto> listPaged(int page, int size, Boolean contentsType, String sortBy) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 50);

        Sort sort = switch (safe(sortBy)) {
            case "views"    -> Sort.by(Sort.Direction.DESC, "views");
            case "feedback" -> Sort.by(Sort.Direction.DESC, "feedbackCount");
            default         -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Post> data = (contentsType == null)
                ? postRepository.findAll(pageable)
                : postRepository.findByContentsType(contentsType, pageable);

        return data.map(PostResponseDto::from);
    }

    /** Pageable을 그대로 받는 목록 (컨트롤러의 /api/posts 에서 사용) */
    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPostList(Boolean contentsType, Pageable pageable) {
        Page<Post> postPage = (contentsType == null)
                ? postRepository.findAll(pageable)
                : postRepository.findByContentsType(contentsType, pageable);
        return postPage.map(PostResponseDto::from);
    }

    @Transactional(readOnly = true)
    public PostResponseDto get(Long id) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시물이 존재하지 않습니다."));
        return PostResponseDto.from(p);
    }

    @Transactional
    public PostResponseDto update(Long id, PostRequestDto dto, Long currentUserId) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시물이 존재하지 않습니다."));

        if (!p.getUserId().equals(currentUserId)) {
            throw new IllegalStateException("수정 권한이 없습니다.");
        }

        validateCodingTest(dto);

        p.setTitle(dto.getTitle());
        p.setContent(dto.getContent());
        p.setCode(dto.getCode());
        p.setSummary(dto.getSummary());
        p.setContentsType(dto.getContentsType());
        p.setGithubRepoUrl(dto.getGithubRepoUrl());
        p.setProblemStatement(dto.getProblemStatement());
        p.setLangTags(normalizeCsv(dto.getLanguages()));
        p.setStackTags(normalizeCsv(dto.getStacks()));

        return PostResponseDto.from(p);
    }

    @Transactional
    public void delete(Long id, Long currentUserId) {
        Post p = postRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("게시물이 존재하지 않습니다."));
        if (!p.getUserId().equals(currentUserId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }
        postRepository.delete(p);
    }

    // ===================== 통합 검색 =====================

    /**
     * 통합 검색 (키워드 + 언어OR + 스택OR + 탭AND + 정렬 + 페이징)
     * - 컨트롤러: /api/posts/search
     * - 키워드: title/content/username LIKE '%q%' (대소문자 무시, %, _ 이스케이프)
     * - 언어/스택: CSV → REGEXP OR 패턴 (예: (^|,)(java|python)(,|$))
     * - 정렬: latest(기본) | views | feedback (쿼리에서 처리)
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDto> discoverAdvanced(
            String q,
            List<String> languages,
            String langsCsv,
            List<String> stacks,
            String stacksCsv,
            Boolean contentsType,
            int page,
            int size,
            String sortKey
    ) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 50);
        Pageable pageable = PageRequest.of(page, size); // 정렬은 네이티브 쿼리에서

        String qLike = buildLikeParam(q);
        String langRegex  = listToRegex(mergeParamsToList(languages, langsCsv));
        String stackRegex = listToRegex(mergeParamsToList(stacks,    stacksCsv));

        Page<Post> data = postRepository.discover(
                qLike,
                contentsType,
                langRegex,
                stackRegex,
                safe(sortKey),
                pageable
        );
        return data.map(PostResponseDto::from);
    }

    // ===================== 좋아요 / 스크랩 =====================

    @Transactional
    public void toggleLike(PostActionDto dto) {
        postLikeRepository.findByUserIdAndPostId(dto.getUserId(), dto.getPostId())
                .ifPresentOrElse(
                        postLikeRepository::delete,
                        () -> {
                            PostLike nl = new PostLike();
                            nl.setUserId(dto.getUserId());
                            nl.setPostId(dto.getPostId());
                            postLikeRepository.save(nl);
                        }
                );
    }

    @Transactional
    public void toggleScrap(PostActionDto dto) {
        PostScrapId key = new PostScrapId(dto.getUserId(), dto.getPostId());
        postScrapRepository.findById(key)
                .ifPresentOrElse(
                        postScrapRepository::delete,
                        () -> {
                            PostScrap ns = new PostScrap();
                            ns.setUserId(dto.getUserId());
                            ns.setPostId(dto.getPostId());
                            postScrapRepository.save(ns);
                        }
                );
    }

    /** 내 스크랩 목록 (username 기준) */
    @Transactional(readOnly = true)
    public List<PostSimpleResponseDto> getMyScrappedPosts(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Long userId = user.getId();

        List<Long> postIds = postScrapRepository.findAllByUserId(userId)
                .stream().map(PostScrap::getPostId).toList();

        if (postIds.isEmpty()) return List.of();

        return postRepository.findAllById(postIds)
                .stream()
                .map(PostSimpleResponseDto::new)
                .collect(Collectors.toList());
    }

    /** 특정 사용자가 등록한 게시물 (최신순) */
    @Transactional(readOnly = true)
    public List<PostSimpleResponseDto> getPostsByUserId(Long userId) {
        // ✔ PostRepository에 아래 메서드가 필요:
        // List<Post> findByUserIdOrderByCreatedAtDesc(Long userId);
        return postRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(PostSimpleResponseDto::new)
                .collect(Collectors.toList());
    }

    /** 인기 게시물 TOP4 (likesCount 내림차순) */
    @Transactional(readOnly = true)
    public List<PostSimpleResponseDto> getPopularPosts(Boolean contentsType) {
        // ✔ PostRepository에 아래 메서드가 필요:
        // List<Post> findTop4ByContentsTypeOrderByLikesCountDesc(Boolean contentsType);
        return postRepository.findTop4ByContentsTypeOrderByLikesCountDesc(contentsType)
                .stream()
                .map(PostSimpleResponseDto::new)
                .collect(Collectors.toList());
    }

    // ===================== AI 피드백 =====================

    @Transactional
    public PostResponseDto generateAiFeedback(Long postId, Long requesterId) {
        if (requesterId != null) {
            log.debug("[AI] generate request by userId={} for postId={}", requesterId, postId);
        }
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물이 존재하지 않습니다."));
        String aiText = aiFeedbackService.generateTextForPost(postId);
        p.setAiFeedback(aiText);
        log.info("[AI] Feedback generated manually for postId={}", postId);
        return PostResponseDto.from(p);
    }

    @Transactional(readOnly = true)
    public String getAiFeedback(Long postId) {
        Post p = postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물이 존재하지 않습니다."));
        return p.getAiFeedback();
    }

    // ===================== Utils =====================

    private void validateCodingTest(PostRequestDto dto) {
        if (Boolean.FALSE.equals(dto.getContentsType())
                && (dto.getProblemStatement() == null || dto.getProblemStatement().isBlank())) {
            throw new IllegalArgumentException("코딩테스트 게시물은 problemStatement(문제 설명)가 필수입니다.");
        }
    }

    /** "Java , python, PYTHON" → "java,python" (빈 결과면 null) */
    private String normalizeCsv(String csv) {
        if (csv == null || csv.isBlank()) return null;
        String normalized = Arrays.stream(csv.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.joining(","));
        return normalized.isEmpty() ? null : normalized;
    }

    /** 배열 + CSV 병합 (언어/스택 공용) */
    private List<String> mergeParamsToList(List<String> arrayParam, String csvParam) {
        List<String> list = new ArrayList<>();
        if (arrayParam != null) list.addAll(arrayParam);
        if (csvParam != null && !csvParam.isBlank()) {
            list.addAll(Arrays.stream(csvParam.split(",")).toList());
        }
        return list.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
                .distinct()
                .collect(Collectors.toList());
    }

    /** ["java","python"] → '(^|,)(java|python)(,|$)' (없으면 null) */
    private String listToRegex(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        String body = values.stream()
                .map(this::escapeRegex)
                .collect(Collectors.joining("|"));
        return "(^|,)(" + body + ")(,|$)";
    }

    /** 정규식 특수문자 이스케이프 */
    private String escapeRegex(String s) {
        return s.replaceAll("([^A-Za-z0-9_\\-])", "\\\\$1");
    }

    /** 정렬 키 기본값 처리 */
    private String safe(String s) {
        return (s == null) ? "latest" : s.toLowerCase();
    }

    /**
     * LIKE 파라미터 생성 (대소문자 무시 + 와일드카드 이스케이프)
     * - 입력 비어있으면 null → WHERE에서 조건 무시
     * - %, _ 는 ESCAPE '\\'와 함께 안전하게 검색되도록 이스케이프
     */
    private String buildLikeParam(String raw) {
        if (raw == null) return null;
        String t = raw.trim().toLowerCase();
        if (t.isEmpty()) return null;
        t = t.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + t + "%";
    }
}