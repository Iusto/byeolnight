package com.byeolnight.controller.news;

import com.byeolnight.domain.entity.News;
import com.byeolnight.domain.repository.NewsRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/news")
@RequiredArgsConstructor
@Tag(name = "🌍 공개 뉴스", description = "우주 뉴스 조회 API")
public class PublicNewsController {
    
    private final NewsRepository newsRepository;
    
    @GetMapping
    @Operation(summary = "우주 뉴스 목록 조회", description = "페이징된 우주 뉴스 목록을 조회합니다")
    public ResponseEntity<Page<News>> getNews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<News> newsPage = newsRepository.findAll(pageable);
        
        return ResponseEntity.ok(newsPage);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "뉴스 상세 조회", description = "특정 뉴스의 상세 정보를 조회합니다")
    public ResponseEntity<News> getNewsById(@PathVariable Long id) {
        return newsRepository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}