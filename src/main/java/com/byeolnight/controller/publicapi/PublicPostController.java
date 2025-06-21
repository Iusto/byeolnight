package com.byeolnight.controller.publicapi;

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
@RequestMapping("/api/public/posts")
@RequiredArgsConstructor
public class PublicPostController {

    private final PostService postService;

    @Operation(summary = "조건별 게시글 목록 조회", description = "카테고리와 정렬 조건에 따라 게시글 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<CommonResponse<Page<PostResponseDto>>> getPosts(
            @RequestParam(required = false) String category,
            @RequestParam(required = false, defaultValue = "recent") String sort,
            Pageable pageable
    ) {
        Page<PostResponseDto> posts = postService.getFilteredPosts(category, sort, pageable);
        return ResponseEntity.ok(CommonResponse.success(posts));
    }

    @Operation(summary = "게시글 단건 조회", description = "게시글 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{id:\\d+}")
    public ResponseEntity<CommonResponse<PostResponseDto>> getPostById(@PathVariable Long id) {
        PostResponseDto response = postService.getPostById(id, null);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
