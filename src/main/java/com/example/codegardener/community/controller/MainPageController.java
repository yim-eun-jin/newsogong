package com.example.codegardener.community.controller;

import com.example.codegardener.community.dto.MainPageResponseDto;
import com.example.codegardener.community.service.MainPageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/main")
public class MainPageController {

    private final MainPageService mainPageService;

    @GetMapping
    public ResponseEntity<MainPageResponseDto> getMainPage() {
        MainPageResponseDto mainPageData = mainPageService.getMainPageData();
        return ResponseEntity.ok(mainPageData);
    }
}