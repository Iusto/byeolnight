package com.byeolnight.controller;

import com.byeolnight.service.post.PostService;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.dto.post.PostResponseDto;
import com.byeolnight.domain.entity.user.User;
import com.byeolnight.infrastructure.common.CommonResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 생성", description = "게시글을 등록합니다.")
    @ApiResponse(responseCode = "200", description = "생성된 게시글 ID 반환")
    @PostMapping
    public ResponseEntity<CommonResponse<Long>> create(@Valid @RequestBody PostRequestDto requestDto,
                                                       @AuthenticationPrincipal User user) {
        Long postId = postService.createPost(requestDto, user);
        return ResponseEntity.ok(CommonResponse.success(postId));
    }

    @Operation(summary = "게시글 단건 조회", description = "게시글 ID로 상세 정보를 조회합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/{id}")
    public ResponseEntity<CommonResponse<PostResponseDto>> getPostById(
            @PathVariable Long id,
            @Parameter(hidden = true) @AuthenticationPrincipal User user
    ) {
        PostResponseDto response = postService.getPostById(id, user);
        return ResponseEntity.ok(CommonResponse.success(response));
    }

    @Operation(summary = "게시글 목록 조회", description = "페이지네이션을 통해 게시글 목록을 조회합니다.")
    @ApiResponse(responseCode = "200", description = "게시글 목록 반환")
    @GetMapping
    public ResponseEntity<CommonResponse<Page<PostResponseDto>>> list(Pageable pageable) {
        return ResponseEntity.ok(CommonResponse.success(postService.getAllPosts(pageable)));
    }

    @Operation(summary = "게시글 수정", description = "본인이 작성한 게시글을 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> update(@PathVariable Long id,
                                                       @Valid @RequestBody PostRequestDto requestDto,
                                                       @AuthenticationPrincipal User user) {
        postService.updatePost(id, requestDto, user);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @Operation(summary = "게시글 삭제", description = "본인이 작성한 게시글을 삭제합니다. (Soft Delete)")
    @DeleteMapping("/{id}")
    public ResponseEntity<CommonResponse<Void>> delete(@PathVariable Long id,
                                                       @AuthenticationPrincipal User user) {
        postService.softDeletePost(id, user);
        return ResponseEntity.ok(CommonResponse.success());
    }

    @Operation(summary = "게시글 추천", description = "게시글을 추천합니다. 중복 추천은 불가합니다.")
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId, @AuthenticationPrincipal User user) {
        postService.likePost(user.getId(), postId);
        return ResponseEntity.ok(CommonResponse.success("추천 완료"));
    }
}
