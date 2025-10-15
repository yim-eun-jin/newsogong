package com.example.codegardener.community.service;

import com.example.codegardener.community.dto.MainPageResponseDto;
import com.example.codegardener.post.dto.PostSimpleResponseDto;
import com.example.codegardener.post.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MainPageService {

    private final PostService postService;

    public MainPageResponseDto getMainPageData() {
        // PostService를 호출하여 각각의 인기 게시물 목록을 가져옴
        List<PostSimpleResponseDto> devPosts = postService.getPopularPosts(true); // true: 개발
        List<PostSimpleResponseDto> codingTestPosts = postService.getPopularPosts(false); // false: 코테

        // Builder를 사용하여 DTO를 생성하고 반환
        return MainPageResponseDto.builder()
                .popularDevPosts(devPosts)
                .popularCodingTestPosts(codingTestPosts)
                .build();
    }
}