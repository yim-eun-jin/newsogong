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
    private final UserRepository userRepository;
    private final AiFeedbackService aiFeedbackService;
    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;


    // ====================== CRUD ======================

    @Transactional
    public PostResponseDto create(PostRequestDto dto) {
        validateCodingTest(dto);

        // 작성자 엔티티 조회 후 연관관계로 설정
        User author = userRepository.findById(dto.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("작성자 정보를 찾을 수 없습니다. userId=" + dto.getUserId()));

        Post p = Post.builder()
                .user(author)
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

    /** 목록(페이징) */
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

        // 연관관계 기준으로 권한 확인
        Long ownerId = (p.getUser() != null) ? p.getUser().getId() : null;
        if (!Objects.equals(ownerId, currentUserId)) {
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

        Long ownerId = (p.getUser() != null) ? p.getUser().getId() : null;
        if (!Objects.equals(ownerId, currentUserId)) {
            throw new IllegalStateException("삭제 권한이 없습니다.");
        }

        postRepository.delete(p);
    }

    // ====================== 통합 검색 ======================

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

        Pageable pageable = PageRequest.of(page, size); // 정렬은 네이티브 쿼리에서 처리

        String qLike = buildLikeParam(q);
        List<String> langList  = mergeParamsToList(languages, langsCsv);
        List<String> stackList = mergeParamsToList(stacks,    stacksCsv);

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

    // ====================== AI 피드백 ======================

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
        return postRepository.findById(postId)
                .orElseThrow(() -> new IllegalArgumentException("게시물이 존재하지 않습니다."))
                .getAiFeedback();
    }

    // ====================== Utils ======================

    private void validateCodingTest(PostRequestDto dto) {
        if (Boolean.FALSE.equals(dto.getContentsType())
                && (dto.getProblemStatement() == null || dto.getProblemStatement().isBlank())) {
            throw new IllegalArgumentException("코딩테스트 게시물은 problemStatement(문제 설명)가 필수입니다.");
        }
    }

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

    private String listToRegex(List<String> values) {
        if (values == null || values.isEmpty()) return null;
        String body = values.stream()
                .map(this::escapeRegex)
                .collect(Collectors.joining("|"));
        return "(^|,)(" + body + ")(,|$)";
    }

    private String escapeRegex(String s) {
        return s.replaceAll("([^A-Za-z0-9_\\-])", "\\\\$1");
    }

    private String safe(String s) {
        return (s == null) ? "latest" : s.toLowerCase();
    }

    private String buildLikeParam(String raw) {
        if (raw == null) return null;
        String t = raw.trim().toLowerCase();
        if (t.isEmpty()) return null;
        t = t.replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + t + "%";
    }

    @Transactional
    public void toggleLike(PostActionDto dto) {
        Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(dto.getUserId(), dto.getPostId());
        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get());
        } else {
            PostLike newLike = new PostLike();
            newLike.setUserId(dto.getUserId());
            newLike.setPostId(dto.getPostId());
            postLikeRepository.save(newLike);
        }
    }

    @Transactional
    public void toggleScrap(PostActionDto dto) {
        Optional<PostScrap> existingScrap = postScrapRepository.findById(new PostScrapId(dto.getUserId(), dto.getPostId()));
        if (existingScrap.isPresent()) {
            postScrapRepository.delete(existingScrap.get());
        } else {
            PostScrap newScrap = new PostScrap();
            newScrap.setUserId(dto.getUserId());
            newScrap.setPostId(dto.getPostId());
            postScrapRepository.save(newScrap);
        }
    }

    @Transactional(readOnly = true)
    public List<PostSimpleResponseDto> getMyScrappedPosts(String username) {
        User user = userRepository.findByUserName(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Long userId = user.getId();

        List<PostScrap> scraps = postScrapRepository.findAllByUserId(userId);

        List<Long> postIds = scraps.stream()
                .map(PostScrap::getPostId)
                .toList();

        if (postIds.isEmpty()) {
            return Collections.emptyList();
        }

        return postRepository.findAllById(postIds).stream()
                .map(PostSimpleResponseDto::new)
                .collect(Collectors.toList());
    }

    // 특정 사용자가 등록한 게시물 조회
    @Transactional(readOnly = true)
    public List<PostSimpleResponseDto> getPostsByUserId(Long userId) {
        List<Post> posts = postRepository.findByUserIdOrderByCreatedAtDesc(userId);
        // stream()과 map()을 사용하여 각 Post 객체를 PostSimpleResponseDto로 변환
        return posts.stream()
                .map(PostSimpleResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<PostResponseDto> getPostList(Boolean contentsType, Pageable pageable) {
        Page<Post> postPage;

        if (contentsType == null) {
            postPage = postRepository.findAll(pageable);
        } else {
            postPage = postRepository.findByContentsType(contentsType, pageable);
        }

        // 조회된 Page<Post>를 Page<PostResponseDto>로 변환하여 반환
        return postPage.map(PostResponseDto::from);
    }

    @Transactional(readOnly = true)
    public List<PostSimpleResponseDto> getPopularPosts(Boolean contentsType) {
        List<Post> popularPosts = postRepository.findTop4ByContentsTypeOrderByLikesCountDesc(contentsType);
        return popularPosts.stream()
                .map(PostSimpleResponseDto::new) // Post를 간단한 DTO로 변환
                .collect(Collectors.toList());
    }
}