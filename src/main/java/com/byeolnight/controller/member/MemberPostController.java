package com.byeolnight.controller.member;

import com.byeolnight.service.post.PostService;
import com.byeolnight.dto.post.PostRequestDto;
import com.byeolnight.infrastructure.common.CommonResponse;
import com.byeolnight.domain.entity.user.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/member/posts")
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class MemberPostController {

    private final PostService postService;

    @Operation(summary = "게시글 생성", description = "게시글을 등록합니다.")
    @PostMapping
    public ResponseEntity<CommonResponse<Long>> create(@Valid @RequestBody PostRequestDto requestDto,
                                                       @AuthenticationPrincipal User user) {
        Long postId = postService.createPost(requestDto, user);
        return ResponseEntity.ok(CommonResponse.success(postId));
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
    @PostMapping("/posts/{postId}/like")
    public ResponseEntity<?> likePost(@PathVariable Long postId,
                                      @AuthenticationPrincipal User user) {
        postService.likePost(user.getId(), postId);
        return ResponseEntity.ok(CommonResponse.success("추천 완료"));
    }
}
