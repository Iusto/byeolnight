package com.byeolnight.controller;

import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.service.post.PostService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/posts") // ✅ 비인증 구간
@RequiredArgsConstructor
public class PublicPostController {

    private final PostService postService;

    @Operation(summary = "게시글 목록 조회", description = "로그인 없이 전체 게시글 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<Page<PostResponseDto>>> list(Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(postService.getAllPosts(pageable)));
    }

    @Operation(summary = "게시글 상세 조회", description = "로그인 없이 게시글 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PostResponseDto>> getPostById(@PathVariable Long id) {
        return ResponseEntity.ok(CommonResponse.success(postService.getPostById(id, null)));
    }
}
