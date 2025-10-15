package com.example.codegardener.post.service;

import com.example.codegardener.ai.service.AiFeedbackService;
import com.example.codegardener.post.domain.Post;
import com.example.codegardener.post.dto.PostRequestDto;
import com.example.codegardener.post.dto.PostResponseDto;
import com.example.codegardener.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostService {

    private final PostRepository postRepository;
    private final AiFeedbackService aiFeedbackService;

    // CRUD

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

    // 목록
     /* 목록(페이징)
     * contentsType: null=전체 / true=개발 / false=코테
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

    // 통합 검색

    /**
     * 통합 검색 (키워드 + 언어OR + 스택OR + 탭AND + 정렬 + 페이징)
     * /posts/search 에서 호출
     * - 키워드: title/content/username LIKE '%q%' (대소문자 무시, %, _ 이스케이프)
     * - 언어/스택: CSV 문자열을 REGEXP OR 패턴으로 변환 (예: (^|,)(java|python)(,|$))
     * - 정렬: latest(기본) | views | feedback (native ORDER BY에서 처리)
     */
    @Transactional(readOnly = true)
    public Page<PostResponseDto> discoverAdvanced(
            String q,
            List<String> languages,   // ?languages=java&languages=python
            String langsCsv,          // 또는 ?langs=java,python
            List<String> stacks,      // ?stacks=spring&stacks=docker
            String stacksCsv,         // 또는 ?tech=spring,docker
            Boolean contentsType,     // 탭 필터
            int page,
            int size,
            String sortKey            // latest | views | feedback
    ) {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), 50);

        // 정렬은 native ORDER BY가 처리
        Pageable pageable = PageRequest.of(page, size);

        // 키워드 1개 (NULL이면 조건 무시) — %, _ 이스케이프 처리
        String qLike = buildLikeParam(q);

        // 언어/스택 파라미터 정규화 (배열 + CSV → OR)
        List<String> langList  = mergeParamsToList(languages, langsCsv);
        List<String> stackList = mergeParamsToList(stacks,    stacksCsv);

        // CSV 칼럼용 REGEXP 패턴: (^|,)(java|python)(,|$)
        String langRegex  = listToRegex(langList);
        String stackRegex = listToRegex(stackList);

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

    // AI 피드백

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

    // 유틸

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
                .map(this::escapeRegex) // 정규식 메타문자 이스케이프
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
     * LIKE용 파라미터 생성 (대소문자 무시 + 와일드카드 이스케이프)
     * - 입력이 비어있으면 null 반환 → WHERE에서 조건 무시
     * - %, _ 는 ESCAPE '\\' 와 함께 안전하게 검색되도록 백슬래시로 이스케이프
     */
    private String buildLikeParam(String raw) {
        if (raw == null) return null;
        String t = raw.trim().toLowerCase();
        if (t.isEmpty()) return null;
        // 백슬래시 → \\\\ , % → \% , _ → \_
        t = t.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + t + "%";
    }
}