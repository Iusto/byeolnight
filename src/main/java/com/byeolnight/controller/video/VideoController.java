package com.byeolnight.controller.video;

import com.byeolnight.service.cinema.CinemaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "우주 영상", description = "YouTube 우주 관련 영상 서비스")
@RestController
@RequestMapping("/api/public/videos")
@RequiredArgsConstructor
@Slf4j
public class VideoController {
    
    private final CinemaService cinemaService;
    
    @Operation(summary = "우주 영상 목록", description = "YouTube에서 우주 관련 영상을 검색합니다")
    @GetMapping("/space")
    public ResponseEntity<List<Map<String, Object>>> getSpaceVideos() {
        log.info("우주 영상 목록 요청");
        
        List<Map<String, Object>> videos = cinemaService.searchSpaceVideos();
        return ResponseEntity.ok(videos);
    }
    
    @Operation(summary = "키워드 영상 검색", description = "특정 키워드로 우주 관련 영상을 검색합니다")
    @GetMapping("/search")
    public ResponseEntity<List<Map<String, Object>>> searchVideos(@RequestParam String keyword) {
        log.info("키워드 영상 검색: {}", keyword);
        
        List<Map<String, Object>> videos = cinemaService.searchVideosByKeyword(keyword);
        return ResponseEntity.ok(videos);
    }
}