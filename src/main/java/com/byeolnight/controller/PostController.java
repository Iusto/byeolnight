package com.byeolnight.controller;

import com.byeolnight.infrastructure.security.CustomUserDetails;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @Operation(summary = "게시글 생성", description = "게시글을 등록합니다.")
    @ApiResponse(responseCode = "200", description = "생성된 게시글 ID 반환")
    @PostMapping
    public ResponseEntity<?> create(@RequestBody PostRequestDto dto,
                                    @AuthenticationPrincipal CustomUserDetails userDetails) {
        log.info("🔥 userDetails: {}", userDetails); // null 체크
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("인증 정보 없음");
        }

        User user = userDetails.getUser(); // 여기서도 null 가능성
        log.info("🔥 user: {}", user);

        postService.createPost(dto, user);
        return ResponseEntity.ok().build();
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
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId, @AuthenticationPrincipal User user) {
        postService.likePost(user.getId(), postId);
        return ResponseEntity.ok(CommonResponse.success("추천 완료"));
    }
}
