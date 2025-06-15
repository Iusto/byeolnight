package com.byeolnight.controller.publicapi;

import com.byeolnight.service.post.PostService;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
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

    @Operation(summary = "게시글 목록 조회", description = "페이지네이션을 통해 게시글 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 목록 반환")
    @GetMapping
    public ResponseEntity<CommonResponse<Page<PostResponseDto>>> list(Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(postService.getAllPosts(pageable)));
    }

    @Operation(summary = "게시글 단건 조회", description = "게시글 ID로 상세 정보를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PostResponseDto>> getPostById(@PathVariable Long id) {
        PostResponseDto response = postService.getPostById(id, null);
        return ResponseEntity.ok(CommonResponse.success(response));
    }
}
