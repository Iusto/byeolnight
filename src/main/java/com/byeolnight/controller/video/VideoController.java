package com.byeolnight.controller.video;

import com.byeolnight.service.cinema.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "🎥 공개 API - 우주 영상", description = "YouTube API 기반 우주 관련 영상 서비스")
@RestController
@RequestMapping("/api/public/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {
    
    private final CinemaService cinemaService;
    
    @Operation(summary = "우주 영상 목록", description = """
    YouTube에서 우주 관련 영상을 검색합니다.
    
    📌 반환 데이터 구조:
    - videoId: YouTube 영상 ID
    - title: 영상 제목 (번역됨)
    - description: 설명 (요약됨)
    - thumbnailUrl: 섬네일 이미지 URL
    - publishedAt: 게시 일시
    - channelTitle: 채널명
    """)
    @ApiResponse(responseCode = "200", description = "영상 목록 조회 성공")
    @GetMapping("/space")
    public ResponseEntity<List<Map<String, Object>>> getSpaceVideos() {
        log.info("우주 영상 목록 요청");
        
        List<Map<String, Object>> videos = cinemaService.searchSpaceVideos();
        return ResponseEntity.ok(videos);
    }
    
    @Operation(summary = "키워드 영상 검색", description = """
    특정 키워드로 우주 관련 영상을 검색합니다.
    
    🔍 검색 예시:
    - "mars" -> 화성 관련 영상
    - "black hole" -> 블랙홀 관련 영상
    - "ISS" -> 국제우주정거장 관련 영상
    """)
    @Parameter(name = "keyword", description = "검색 키워드 (영어 권장)", example = "mars exploration", required = true)
    @ApiResponse(responseCode = "200", description = "검색 결과 조회 성공")
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchVideos(@RequestParam String keyword) {
        log.info("키워드 영상 검색: {}", keyword);
        
        List<Map<String, Object>> videos = cinemaService.searchVideosByKeyword(keyword);
        return ResponseEntity.ok(videos);
    }
}