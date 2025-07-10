package com.byeolnight.controller.admin;

import com.byeolnight.service.file.GoogleVisionService;
import com.byeolnight.service.video.YouTubeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Tag(name = "Google API 테스트", description = "Google Vision API 및 YouTube API 테스트")
@RestController
@RequestMapping("/api/admin/google-test")
@RequiredArgsConstructor
@Slf4j
public class GoogleApiTestController {
    
    private final GoogleVisionService googleVisionService;
    private final YouTubeService youTubeService;
    
    @Operation(summary = "이미지 검열 테스트", description = "Google Vision API로 이미지 안전성을 검사합니다")
    @PostMapping("/vision/check")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> testImageSafety(@RequestParam("image") MultipartFile image) {
        try {
            if (image.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "이미지 파일이 필요합니다"));
            }
            
            byte[] imageBytes = image.getBytes();
            boolean isSafe = googleVisionService.isImageSafe(imageBytes);
            
            return ResponseEntity.ok(Map.of(
                "filename", image.getOriginalFilename(),
                "size", image.getSize(),
                "isSafe", isSafe,
                "message", isSafe ? "안전한 이미지입니다" : "부적절한 콘텐츠가 감지되었습니다"
            ));
            
        } catch (Exception e) {
            log.error("이미지 검열 테스트 중 오류", e);
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @Operation(summary = "YouTube 영상 검색 테스트", description = "YouTube API로 우주 관련 영상을 검색합니다")
    @GetMapping("/youtube/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> testYouTubeSearch(@RequestParam(defaultValue = "우주") String keyword) {
        log.info("YouTube 검색 테스트: {}", keyword);
        
        List<Map<String, Object>> videos = youTubeService.searchVideosByKeyword(keyword);
        return ResponseEntity.ok(videos);
    }
    
    @Operation(summary = "우주 영상 목록 테스트", description = "기본 우주 관련 영상 목록을 가져옵니다")
    @GetMapping("/youtube/space")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Map<String, Object>>> testSpaceVideos() {
        log.info("우주 영상 목록 테스트");
        
        List<Map<String, Object>> videos = youTubeService.searchSpaceVideos();
        return ResponseEntity.ok(videos);
    }
}