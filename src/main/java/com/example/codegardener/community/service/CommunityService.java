package com.example.codegardener.community.service;

import com.example.codegardener.community.dto.PostActionDto;
import com.example.codegardener.community.entity.PostLike;
import com.example.codegardener.community.entity.PostScrap;
import com.example.codegardener.community.repository.PostLikeRepository;
import com.example.codegardener.community.repository.PostScrapRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CommunityService {

    private final PostLikeRepository postLikeRepository;
    private final PostScrapRepository postScrapRepository;

    // 게시물 좋아요 토글 (추가/취소)
    @Transactional
    public void toggleLike(PostActionDto dto) {
        Optional<PostLike> existingLike = postLikeRepository.findByUserIdAndPostId(dto.getUserId(), dto.getPostId());

        if (existingLike.isPresent()) {
            postLikeRepository.delete(existingLike.get()); // 취소
        } else {
            PostLike newLike = new PostLike();
            newLike.setUserId(dto.getUserId());
            newLike.setPostId(dto.getPostId());
            postLikeRepository.save(newLike); // 추가
        }
    }

    // 게시물 스크랩 토글 (추가/취소)
    @Transactional
    public void toggleScrap(PostActionDto dto) {
        Optional<PostScrap> existingScrap = postScrapRepository.findById(new com.example.codegardener.community.entity.PostScrapId(dto.getUserId(), dto.getPostId()));

        if (existingScrap.isPresent()) {
            postScrapRepository.delete(existingScrap.get()); // 취소
        } else {
            PostScrap newScrap = new PostScrap();
            newScrap.setUserId(dto.getUserId());
            newScrap.setPostId(dto.getPostId());
            postScrapRepository.save(newScrap); // 추가
        }
    }

    // 특정 게시물의 좋아요 개수 조회
    @Transactional(readOnly = true)
    public long getLikeCount(Long postId) {
        return postLikeRepository.countByPostId(postId);
    }

    // 특정 게시물의 스크랩 개수 조회
    @Transactional(readOnly = true)
    public long getScrapCount(Long postId) {
        return postScrapRepository.countByPostId(postId);
    }
}