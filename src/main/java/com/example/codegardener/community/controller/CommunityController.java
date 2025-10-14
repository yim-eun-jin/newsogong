package com.example.codegardener.community.controller;

import com.example.codegardener.community.dto.PostActionDto;
import com.example.codegardener.community.service.CommunityService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/community")
public class CommunityController {

    private final CommunityService communityService;

    // 게시물 좋아요 토글 API
    @PostMapping("/likes")
    public ResponseEntity<Void> toggleLike(@RequestBody PostActionDto postActionDto) {
        communityService.toggleLike(postActionDto);
        return ResponseEntity.ok().build();
    }

    // 게시물 스크랩 토글 API
    @PostMapping("/scraps")
    public ResponseEntity<Void> toggleScrap(@RequestBody PostActionDto postActionDto) {
        communityService.toggleScrap(postActionDto);
        return ResponseEntity.ok().build();
    }

    // 특정 게시물의 좋아요 수 조회 API
    @GetMapping("/likes/{postId}/count")
    public ResponseEntity<Long> getLikeCount(@PathVariable Long postId) {
        long count = communityService.getLikeCount(postId);
        return ResponseEntity.ok(count);
    }

    // 특정 게시물의 스크랩 수 조회 API
    @GetMapping("/scraps/{postId}/count")
    public ResponseEntity<Long> getScrapCount(@PathVariable Long postId) {
        long count = communityService.getScrapCount(postId);
        return ResponseEntity.ok(count);
    }
}